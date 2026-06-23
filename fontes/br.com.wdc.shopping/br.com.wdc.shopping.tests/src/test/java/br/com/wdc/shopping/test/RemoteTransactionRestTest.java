package br.com.wdc.shopping.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.persistence.client.RestTransactionService;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

/**
 * Verifica o <b>coordenador de transação remota</b> dirigido pelo cliente sobre REST: várias escritas HTTP
 * participam da mesma transação física no servidor (mesmo {@code txId}), comitando ou revertendo atomicamente.
 *
 * <p>
 * Usa o {@code TestEnvironment} no modo REST (Javalin embarcado + repositórios HTTP) e um
 * {@link RestTransactionService} sobre o mesmo transporte dos repositórios.
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
        var p = new Product();
        p.name = name;
        p.price = 9.99;
        p.description = name + " desc";
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

        assertNotNull("p1 deveria ter sido comitado", env.productRepo().fetchById(p1.id, null));
        assertNotNull("p2 deveria ter sido comitado", env.productRepo().fetchById(p2.id, null));
    }

    @Test
    public void setRollbackOnly_discardsWrites() {
        var p = product("rtx-rollback-only");

        tx().required(t -> {
            env.productRepo().insert(p);
            t.setRollbackOnly();
        });

        assertNull("setRollbackOnly deveria reverter", env.productRepo().fetchById(p.id, null));
    }

    @Test
    public void headerlessWrite_whileRemoteTxOpen_isRejected() {
        // Cliente abre uma transação remota mas, por falha de propagação, deixa de enviar o X-Tx-Id nas escritas
        // seguintes. O servidor deve REJEITAR a escrita — em vez de autocommitá-la fora da transação, o que deixaria
        // um registro órfão (quebra de atomicidade). begin e escrita partem do mesmo transporte → mesmo X-Client-Id
        // (mesmo dono), então a guarda correlaciona a escrita à transação aberta do cliente.
        env.transport().postJson("/api/tx/begin", "{}"); // transação remota agora aberta para este cliente

        try {
            env.productRepo().insert(product("rtx-guard-orphan")); // escrita SEM X-Tx-Id
            fail("escrita sem X-Tx-Id com transação remota aberta deveria ser rejeitada");
        } catch (BusinessException expected) {
            // esperado: 409 Conflict (conflito de estado transacional), não 403 (autorização)
            assertTrue("guarda deveria responder 409, veio: " + expected.getMessage(),
                    expected.getMessage().contains("409"));
        }
        // A transação aberta é abandonada de propósito; o coordenador a reverte por timeout (reaper) e o
        // TestEnvironment fecha o datasource no teardown da classe.
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
            fail("a exceção deveria ter sido repropagada");
        } catch (IllegalStateException expected) {
            // esperado
        }

        assertNull("p1 deveria ter sido revertido junto", env.productRepo().fetchById(p1.id, null));
        assertNull("p2 deveria ter sido revertido junto", env.productRepo().fetchById(p2.id, null));
    }
}
