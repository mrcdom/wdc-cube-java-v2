package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Before;
import org.junit.Test;

import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.persistence.transaction.TransactionScope;
import br.com.wdc.framework.persistence.transaction.TransactionServiceImpl;

/**
 * {@link TransactionService#afterCommit} in JDBC mode: a task runs after the commit of its physical transaction, outside
 * it, and never after a rollback.
 */
public class AfterCommitTest {

    private final List<String> ran = new ArrayList<>();
    private TransactionService tx;

    @Before
    public void setUp() {
        var dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:after-commit;DB_CLOSE_DELAY=-1");
        tx = new TransactionServiceImpl(() -> dataSource);
    }

    private Runnable record(String name) {
        return () -> ran.add(name + (TransactionScope.isActive() ? " in a transaction" : ""));
    }

    @Test
    public void runsAfterTheCommit_outsideTheTransaction() {
        tx.required(t -> {
            tx.afterCommit(record("a"));
            tx.afterCommit(record("b"));
            assertEquals(List.of(), ran);
        });

        assertEquals(List.of("a", "b"), ran);
    }

    @Test
    public void isDiscarded_whenTheWorkThrows() {
        try {
            tx.required(t -> {
                tx.afterCommit(record("a"));
                throw new IllegalStateException("boom");
            });
            fail("the exception should have been rethrown");
        } catch (IllegalStateException expected) {
            // expected
        }

        assertEquals(List.of(), ran);
    }

    @Test
    public void isDiscarded_onSetRollbackOnly() {
        tx.required(t -> {
            tx.afterCommit(record("a"));
            t.setRollbackOnly();
        });

        assertEquals(List.of(), ran);
    }

    @Test
    public void withoutATransaction_runsAtOnce() {
        tx.afterCommit(record("a"));

        assertEquals(List.of("a"), ran);
    }

    @Test
    public void inAJoinedTransaction_waitsForTheOuterCommit() {
        tx.required(outer -> {
            tx.required(inner -> tx.afterCommit(record("inner")));
            assertEquals(List.of(), ran);
        });

        assertEquals(List.of("inner"), ran);
    }

    @Test
    public void inRequiresNew_runsAtItsCommit_evenIfTheOuterRollsBack() {
        tx.required(outer -> {
            tx.afterCommit(record("outer"));
            tx.requiresNew(inner -> tx.afterCommit(record("inner")));
            assertEquals(List.of("inner"), ran);
            outer.setRollbackOnly();
        });

        assertEquals(List.of("inner"), ran);
    }

    @Test
    public void aFailingTask_doesNotStopTheNextOnes() {
        tx.required(t -> {
            tx.afterCommit(() -> {
                throw new IllegalStateException("task failed");
            });
            tx.afterCommit(record("b"));
        });

        assertEquals(List.of("b"), ran);
        assertFalse(TransactionScope.isActive());
    }
}
