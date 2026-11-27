package br.com.wdc.store.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.persistence.client.RestTransactionService;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * Verifies the client-driven <b>remote transaction coordinator</b> over REST: several HTTP writes take part in the
 * same physical transaction on the server (same {@code txId}), committing or rolling back atomically.
 *
 * <p>
 * Uses the {@code TestEnvironment} in REST mode (embedded Javalin + HTTP repositories) and a
 * {@link RestTransactionService} over the same transport as the repositories.
 * </p>
 */
public class RemoteTransactionRestTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    private RestTransactionService tx() {
        return new RestTransactionService(env.transport());
    }

    private static Product product(String name) {
        var p = new Product()
                .withName(name)
                .withPrice(9.99)
                .withDescription(name + " desc");
        return p;
    }

    @Test
    public void commit_persistsAllWrites() {
        var p1 = product("rtx-commit-1");
        var p2 = product("rtx-commit-2");

        tx().required(t -> {
            env.productRepo().insert(p1);
            env.productRepo().insert(p2);
        });

        assertNotNull("p1 should have been committed", env.productRepo().fetchById(p1.id(), null));
        assertNotNull("p2 should have been committed", env.productRepo().fetchById(p2.id(), null));
    }

    @Test
    public void setRollbackOnly_discardsWrites() {
        var p = product("rtx-rollback-only");

        tx().required(t -> {
            env.productRepo().insert(p);
            t.setRollbackOnly();
        });

        assertNull("setRollbackOnly should roll back", env.productRepo().fetchById(p.id(), null));
    }

    @Test
    public void headerlessWrite_whileRemoteTxOpen_isRejected() {
        // The client opens a remote transaction but, due to a propagation failure, does not send the X-Tx-Id on the
        // following writes. The server must REJECT the write — instead of autocommitting it outside the transaction,
        // which would leave an orphan record (broken atomicity). begin and write come from the same transport → same
        // X-Client-Id (same owner), so the guard correlates the write with the client's open transaction.
        env.transport().postJson("/api/tx/begin", "{}"); // remote transaction open for this client

        try {
            env.productRepo().insert(product("rtx-guard-orphan")); // write WITHOUT X-Tx-Id
            fail("a write without X-Tx-Id while a remote transaction is open should be rejected");
        } catch (TransactionConflictException expected) {
            // expected: 409 Conflict (transactional state conflict), not 403 (authorization) — comes back as the server's exception
        }
        // The open transaction is abandoned on purpose; the coordinator rolls it back on timeout (reaper) and the
        // TestEnvironment closes the datasource in the class teardown.
    }

    @Test
    public void exception_rollsBackAllWritesAtomically() {
        var p1 = product("rtx-atomic-1");
        var p2 = product("rtx-atomic-2");

        try {
            tx().required(t -> {
                env.productRepo().insert(p1);
                env.productRepo().insert(p2);
                throw new IllegalStateException("boom");
            });
            fail("the exception should have been rethrown");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertNull("p1 should have been rolled back too", env.productRepo().fetchById(p1.id(), null));
        assertNull("p2 should have been rolled back too", env.productRepo().fetchById(p2.id(), null));
    }

    @Test
    public void afterCommit_runsAfterTheRemoteCommit_andNotAfterARollback() {
        var ran = new ArrayList<String>();

        tx().required(t -> {
            env.productRepo().insert(product("rtx-after-commit"));
            tx().afterCommit(() -> ran.add("committed"));
            assertEquals(List.of(), ran);
        });
        tx().required(t -> {
            tx().afterCommit(() -> ran.add("rolled back"));
            t.setRollbackOnly();
        });

        assertEquals(List.of("committed"), ran);
    }

    @Test
    public void imageChangedInARemoteTransaction_isServedAfterTheCommit() {
        var id = DBReset.COFFEE_MAKER_ID;
        var before = env.productRepo().fetchImage(id); // the server now holds it in its cache
        var changed = new byte[] { 1, 2, 3 };

        tx().required(t -> env.productRepo().updateImage(id, changed));

        assertArrayEquals(changed, env.productRepo().fetchImage(id));
        assertNotNull(before);
    }

    @Test
    public void imageChangedInARolledBackTransaction_keepsTheOldOne() {
        var id = DBReset.COFFEE_MAKER_ID;
        var before = env.productRepo().fetchImage(id);

        tx().required(t -> {
            env.productRepo().updateImage(id, new byte[] { 1, 2, 3 });
            t.setRollbackOnly();
        });

        assertArrayEquals(before, env.productRepo().fetchImage(id));
    }

    /** A transaction this server does not know — ended, reaped, or held by another instance — is gone: 410. */
    @Test
    public void aCallInAnUnknownTransaction_isGone() throws Exception {
        var request = HttpRequest.newBuilder(URI.create(env.baseUrl() + "/api/tx/commit"))
                .header("X-Tx-Id", "no-such-transaction")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        var response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());

        assertEquals(410, response.statusCode());
    }
}
