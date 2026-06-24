package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.concurrent.Callable;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TransactionLimitExceededException;
import br.com.wdc.framework.persistence.transaction.JtaTransactionManager;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinatorImpl;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionOptions;
import br.com.wdc.framework.persistence.transaction.TransactionScope;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * Exercita o {@link RemoteTransactionCoordinator} no <b>modo JTA</b> (Narayana + H2 XA), com cada fase
 * (begin / escrita / commit-rollback) numa <b>thread diferente</b> — reproduzindo o cenário real do servidor REST,
 * onde requisições da mesma transação são atendidas por threads distintas do pool. Valida o suspend/resume da
 * transação JTA entre threads.
 */
public class RemoteTransactionCoordinatorJtaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:rtx-coord;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static AgroalDataSource dataSource;
    private static RemoteTransactionCoordinator coordinator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // TransactionManager Narayana (ObjectStore em target/)
        var objectStoreDir = new File("target/rtx-coord-store").getAbsolutePath();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());
        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());
        JtaTransactionManager.BEAN.set(com.arjuna.ats.jta.TransactionManager.transactionManager());

        // DataSource H2 XA enlistado no TM
        var tsr = jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();
        dataSource = AgroalDataSource.from(new AgroalDataSourceConfigurationSupplier()
                .connectionPoolConfiguration(pool -> pool
                        .maxSize(8)
                        .minSize(1)
                        .initialSize(1)
                        .transactionIntegration(
                                new NarayanaTransactionIntegration(JtaTransactionManager.get(), tsr))
                        .connectionFactoryConfiguration(factory -> factory
                                .connectionProviderClass(org.h2.jdbcx.JdbcDataSource.class)
                                .jdbcProperty("url", JDBC_URL)
                                .credential(new NamePrincipal("sa"))
                                .credential(new SimplePassword("sa")))));

        coordinator = new RemoteTransactionCoordinatorImpl(() -> dataSource);

        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS RTX (id INT PRIMARY KEY)");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        JtaTransactionManager.BEAN.set(null);
    }

    @Test
    public void commitAcrossThreads_persistsAllWrites() throws Exception {
        var txId = onThread(() -> coordinator.begin(null));

        // requisição 1 (thread B): primeira escrita
        onThread(() -> {
            coordinator.resume(txId, null);
            try {
                insert(10);
            } finally {
                coordinator.suspend(txId);
            }
            return null;
        });

        // requisição 2 (thread C): segunda escrita, mesma transação
        onThread(() -> {
            coordinator.resume(txId, null);
            try {
                insert(11);
            } finally {
                coordinator.suspend(txId);
            }
            return null;
        });

        // commit (thread D)
        onThread(() -> {
            coordinator.commit(txId, null);
            return null;
        });

        assertEquals("ambas as escritas deveriam ter sido comitadas", 1, count(10));
        assertEquals(1, count(11));
    }

    @Test
    public void rollbackAcrossThreads_discardsAllWrites() throws Exception {
        var txId = onThread(() -> coordinator.begin(null));

        onThread(() -> {
            coordinator.resume(txId, null);
            try {
                insert(20);
                insert(21);
            } finally {
                coordinator.suspend(txId);
            }
            return null;
        });

        onThread(() -> {
            coordinator.rollback(txId, null);
            return null;
        });

        assertEquals("escritas deveriam ter sido revertidas", 0, count(20));
        assertEquals(0, count(21));
    }

    @Test
    public void ownerMismatch_isRejected() throws Exception {
        var txId = onThread(() -> coordinator.begin("user-1"));
        try {
            // outro usuário tenta finalizar a transação alheia
            coordinator.commit(txId, "user-2");
            fail("commit com dono divergente deveria ser rejeitado");
        } catch (AccessDeniedException expected) {
            // esperado
        } finally {
            coordinator.rollback(txId, "user-1"); // dono correto limpa
        }
    }

    @Test
    public void hasOpenTransactionForOwner_tracksOpenTransactions() throws Exception {
        assertFalse("sem transação aberta para o dono", coordinator.hasOpenTransactionForOwner("owner-A"));

        var txId = onThread(() -> coordinator.begin("owner-A"));
        try {
            assertTrue("dono com transação aberta", coordinator.hasOpenTransactionForOwner("owner-A"));
            assertFalse("outro dono não casa", coordinator.hasOpenTransactionForOwner("owner-B"));
            assertFalse("dono nulo nunca casa", coordinator.hasOpenTransactionForOwner(null));
        } finally {
            onThread(() -> {
                coordinator.rollback(txId, "owner-A");
                return null;
            });
        }

        assertFalse("após finalizar, não há mais transação aberta",
                coordinator.hasOpenTransactionForOwner("owner-A"));
    }

    @Test
    public void exceedingMaxOpen_isRejected() throws Exception {
        // coordenador com teto GLOBAL = 2 (por dono no default, para isolar o global)
        var limited = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults().withMaxOpen(2));
        var t1 = onThread(() -> limited.begin("o"));
        var t2 = onThread(() -> limited.begin("o"));
        try {
            onThread(() -> limited.begin("o"));
            fail("3ª transação deveria estourar o teto global");
        } catch (RuntimeException e) {
            assertTrue("esperado TransactionLimitExceededException, veio: " + e.getCause(),
                    e.getCause() instanceof TransactionLimitExceededException);
        } finally {
            onThread(() -> { limited.rollback(t1, "o"); return null; });
            onThread(() -> { limited.rollback(t2, "o"); return null; });
        }
    }

    @Test
    public void exceedingMaxOpenPerOwner_isRejected() throws Exception {
        // teto POR DONO = 2 (global no default)
        var limited = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults().withMaxOpenPerOwner(2));
        var a = onThread(() -> limited.begin("owner-X"));
        var b = onThread(() -> limited.begin("owner-X"));
        try {
            onThread(() -> limited.begin("owner-X"));
            fail("3ª tx do mesmo dono deveria estourar o teto por dono");
        } catch (RuntimeException e) {
            assertTrue("esperado TransactionLimitExceededException, veio: " + e.getCause(),
                    e.getCause() instanceof TransactionLimitExceededException);
        } finally {
            onThread(() -> { limited.rollback(a, "owner-X"); return null; });
            onThread(() -> { limited.rollback(b, "owner-X"); return null; });
        }
        // outro dono não é afetado pelo teto de owner-X
        var c = onThread(() -> limited.begin("owner-Y"));
        onThread(() -> { limited.rollback(c, "owner-Y"); return null; });
    }

    @Test
    public void absoluteLifetime_reapsLongLivedTransaction() throws Exception {
        // idle no default (60s), tempo de vida absoluto curto (50ms): isola a expiração por lifetime
        var shortLife = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults().withMaxLifetimeMs(50L));
        var txId = onThread(() -> shortLife.begin("o"));
        assertTrue("tx recém-aberta deveria existir", shortLife.exists(txId));

        Thread.sleep(120); // ultrapassa o lifetime absoluto, mas não o idle

        // o reaper roda na varredura preguiçosa do begin
        var other = onThread(() -> shortLife.begin("o2"));
        assertFalse("tx além do tempo de vida absoluto deveria ter sido revertida", shortLife.exists(txId));

        onThread(() -> { shortLife.rollback(other, "o2"); return null; });
    }

    @Test
    public void commitIsIdempotent_andStatusReflectsOutcome() throws Exception {
        var txId = onThread(() -> coordinator.begin("o"));
        assertEquals("open", coordinator.status(txId, "o"));

        onThread(() -> { coordinator.commit(txId, "o"); return null; });
        assertEquals("committed", coordinator.status(txId, "o"));

        // retry do commit (resposta anterior "perdida") → no-op de sucesso, mesmo desfecho
        onThread(() -> { coordinator.commit(txId, "o"); return null; });
        assertEquals("committed", coordinator.status(txId, "o"));
    }

    @Test
    public void commitAfterRollback_conflicts() throws Exception {
        var txId = onThread(() -> coordinator.begin("o"));
        onThread(() -> { coordinator.rollback(txId, "o"); return null; });
        try {
            onThread(() -> { coordinator.commit(txId, "o"); return null; });
            fail("commit de uma tx já revertida deveria conflitar");
        } catch (RuntimeException e) {
            assertTrue("esperado TransactionConflictException, veio: " + e.getCause(),
                    e.getCause() instanceof TransactionConflictException);
        }
    }

    @Test
    public void statusUnknown_forUnseenTxId() {
        assertEquals("unknown", coordinator.status("nao-existe", "o"));
    }

    @Test
    public void stats_reflectGaugesAndCounters() throws Exception {
        // coordenador fresco → contadores determinísticos
        var c = new RemoteTransactionCoordinatorImpl(() -> dataSource);
        assertEquals(0, c.stats().openNow());
        assertEquals(0L, c.stats().begun());

        var tx1 = onThread(() -> c.begin("o"));
        var tx2 = onThread(() -> c.begin("o"));
        var open = c.stats();
        assertEquals("2 transações abertas", 2, open.openNow());
        assertEquals("1 dono com tx aberta", 1, open.ownersWithOpen());
        assertEquals(2L, open.begun());

        onThread(() -> { c.commit(tx1, "o"); return null; });
        onThread(() -> { c.rollback(tx2, "o"); return null; });
        var done = c.stats();
        assertEquals("nenhuma aberta após finalizar", 0, done.openNow());
        assertEquals(1L, done.committed());
        assertEquals(1L, done.rolledBack());
    }

    // -------------------------------------------------------------------------

    /** Escreve usando a conexão da transação resumida na thread corrente. */
    private static void insert(int id) {
        try {
            var conn = TransactionScope.current().connection();
            try (var ps = conn.prepareStatement("INSERT INTO RTX (id) VALUES (?)")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Conta fora de qualquer transação (vê apenas o comitado). */
    private static int count(int id) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("SELECT COUNT(*) FROM RTX WHERE id = ?")) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Executa a tarefa numa thread nova e devolve o resultado — força o cross-thread do suspend/resume JTA. */
    private static <T> T onThread(Callable<T> task) throws Exception {
        var result = new Object[1];
        var error = new Throwable[1];
        var thread = new Thread(() -> {
            try {
                result[0] = task.call();
            } catch (Throwable t) {
                error[0] = t;
            }
        });
        thread.start();
        thread.join();
        if (error[0] != null) {
            throw new RuntimeException("Falha na thread de transação", error[0]);
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }
}
