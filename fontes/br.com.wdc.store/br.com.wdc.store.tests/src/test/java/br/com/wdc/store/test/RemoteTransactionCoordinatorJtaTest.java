package br.com.wdc.store.test;

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
 * Exercises the {@link RemoteTransactionCoordinator} in <b>JTA mode</b> (Narayana + H2 XA), with each phase
 * (begin / write / commit-rollback) on a <b>different thread</b> — reproducing the real REST server scenario, where
 * requests of the same transaction are served by different pool threads. Validates the suspend/resume of the JTA
 * transaction across threads.
 */
public class RemoteTransactionCoordinatorJtaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:rtx-coord;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static AgroalDataSource dataSource;
    private static RemoteTransactionCoordinator coordinator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Narayana TransactionManager (ObjectStore under target/)
        var objectStoreDir = new File("target/rtx-coord-store").getAbsolutePath();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());
        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());
        JtaTransactionManager.BEAN.set(com.arjuna.ats.jta.TransactionManager.transactionManager());

        // H2 XA DataSource enlisted in the TM
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

        // request 1 (thread B): first write
        onThread(() -> {
            coordinator.resume(txId, null);
            try {
                insert(10);
            } finally {
                coordinator.suspend(txId);
            }
            return null;
        });

        // request 2 (thread C): second write, same transaction
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

        assertEquals("both writes should have been committed", 1, count(10));
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

        assertEquals("writes should have been rolled back", 0, count(20));
        assertEquals(0, count(21));
    }

    @Test
    public void ownerMismatch_isRejected() throws Exception {
        var txId = onThread(() -> coordinator.begin("user-1"));
        try {
            // another user tries to finish someone else's transaction
            coordinator.commit(txId, "user-2");
            fail("commit with a mismatched owner should be rejected");
        } catch (AccessDeniedException expected) {
            // expected
        } finally {
            coordinator.rollback(txId, "user-1"); // the rightful owner cleans up
        }
    }

    @Test
    public void hasOpenTransactionForOwner_tracksOpenTransactions() throws Exception {
        assertFalse("no open transaction for the owner", coordinator.hasOpenTransactionForOwner("owner-A"));

        var txId = onThread(() -> coordinator.begin("owner-A"));
        try {
            assertTrue("owner with an open transaction", coordinator.hasOpenTransactionForOwner("owner-A"));
            assertFalse("another owner does not match", coordinator.hasOpenTransactionForOwner("owner-B"));
            assertFalse("a null owner never matches", coordinator.hasOpenTransactionForOwner(null));
        } finally {
            onThread(() -> {
                coordinator.rollback(txId, "owner-A");
                return null;
            });
        }

        assertFalse("after finishing, there is no open transaction",
                coordinator.hasOpenTransactionForOwner("owner-A"));
    }

    @Test
    public void exceedingMaxOpen_isRejected() throws Exception {
        // coordinator with a GLOBAL cap = 2 (per-owner left at the default, to isolate the global one)
        var limited = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults()
                        .withMaxOpen(2));
        var t1 = onThread(() -> limited.begin("o"));
        var t2 = onThread(() -> limited.begin("o"));
        try {
            onThread(() -> limited.begin("o"));
            fail("the 3rd transaction should exceed the global cap");
        } catch (RuntimeException e) {
            assertTrue("expected TransactionLimitExceededException, got: " + e.getCause(),
                    e.getCause() instanceof TransactionLimitExceededException);
        } finally {
            onThread(() -> { limited.rollback(t1, "o"); return null; });
            onThread(() -> { limited.rollback(t2, "o"); return null; });
        }
    }

    @Test
    public void exceedingMaxOpenPerOwner_isRejected() throws Exception {
        // PER-OWNER cap = 2 (global left at the default)
        var limited = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults()
                        .withMaxOpenPerOwner(2));
        var a = onThread(() -> limited.begin("owner-X"));
        var b = onThread(() -> limited.begin("owner-X"));
        try {
            onThread(() -> limited.begin("owner-X"));
            fail("the 3rd tx of the same owner should exceed the per-owner cap");
        } catch (RuntimeException e) {
            assertTrue("expected TransactionLimitExceededException, got: " + e.getCause(),
                    e.getCause() instanceof TransactionLimitExceededException);
        } finally {
            onThread(() -> { limited.rollback(a, "owner-X"); return null; });
            onThread(() -> { limited.rollback(b, "owner-X"); return null; });
        }
        // another owner is not affected by owner-X's cap
        var c = onThread(() -> limited.begin("owner-Y"));
        onThread(() -> { limited.rollback(c, "owner-Y"); return null; });
    }

    @Test
    public void absoluteLifetime_reapsLongLivedTransaction() throws Exception {
        // idle at the default (60s), short absolute lifetime (50ms): isolates expiration by lifetime
        var shortLife = new RemoteTransactionCoordinatorImpl(() -> dataSource,
                RemoteTransactionOptions.defaults()
                        .withMaxLifetimeMs(50L));
        var txId = onThread(() -> shortLife.begin("o"));
        assertTrue("a freshly opened tx should exist", shortLife.exists(txId));

        Thread.sleep(120); // exceeds the absolute lifetime, but not the idle timeout

        // the reaper runs in the lazy sweep of begin
        var other = onThread(() -> shortLife.begin("o2"));
        assertFalse("a tx past its absolute lifetime should have been rolled back", shortLife.exists(txId));

        onThread(() -> { shortLife.rollback(other, "o2"); return null; });
    }

    @Test
    public void commitIsIdempotent_andStatusReflectsOutcome() throws Exception {
        var txId = onThread(() -> coordinator.begin("o"));
        assertEquals("open", coordinator.status(txId, "o"));

        onThread(() -> { coordinator.commit(txId, "o"); return null; });
        assertEquals("committed", coordinator.status(txId, "o"));

        // commit retry (previous response "lost") → successful no-op, same outcome
        onThread(() -> { coordinator.commit(txId, "o"); return null; });
        assertEquals("committed", coordinator.status(txId, "o"));
    }

    @Test
    public void commitAfterRollback_conflicts() throws Exception {
        var txId = onThread(() -> coordinator.begin("o"));
        onThread(() -> { coordinator.rollback(txId, "o"); return null; });
        try {
            onThread(() -> { coordinator.commit(txId, "o"); return null; });
            fail("committing an already rolled-back tx should conflict");
        } catch (RuntimeException e) {
            assertTrue("expected TransactionConflictException, got: " + e.getCause(),
                    e.getCause() instanceof TransactionConflictException);
        }
    }

    @Test
    public void statusUnknown_forUnseenTxId() {
        assertEquals("unknown", coordinator.status("does-not-exist", "o"));
    }

    @Test
    public void stats_reflectGaugesAndCounters() throws Exception {
        // fresh coordinator → deterministic counters
        var c = new RemoteTransactionCoordinatorImpl(() -> dataSource);
        assertEquals(0, c.stats().openNow());
        assertEquals(0L, c.stats().begun());

        var tx1 = onThread(() -> c.begin("o"));
        var tx2 = onThread(() -> c.begin("o"));
        var open = c.stats();
        assertEquals("2 open transactions", 2, open.openNow());
        assertEquals("1 owner with an open tx", 1, open.ownersWithOpen());
        assertEquals(2L, open.begun());

        onThread(() -> { c.commit(tx1, "o"); return null; });
        onThread(() -> { c.rollback(tx2, "o"); return null; });
        var done = c.stats();
        assertEquals("none open after finishing", 0, done.openNow());
        assertEquals(1L, done.committed());
        assertEquals(1L, done.rolledBack());
    }

    // -------------------------------------------------------------------------

    /** Writes using the connection of the transaction resumed on the current thread. */
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

    /** Counts outside any transaction (sees only committed data). */
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

    /** Runs the task on a new thread and returns the result — forces the JTA suspend/resume across threads. */
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
            throw new RuntimeException("Failure in the transaction thread", error[0]);
        }
        @SuppressWarnings("unchecked")
        T typed = (T) result[0];
        return typed;
    }
}
