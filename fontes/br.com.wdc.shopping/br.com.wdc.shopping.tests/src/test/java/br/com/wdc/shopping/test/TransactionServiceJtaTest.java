package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.arjuna.ats.arjuna.common.arjPropertyManager;
import com.arjuna.ats.internal.jta.transaction.arjunacore.TransactionManagerImple;
import com.arjuna.ats.internal.jta.transaction.arjunacore.UserTransactionImple;
import com.arjuna.ats.jta.common.jtaPropertyManager;

import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.persistence.transaction.JtaTransactionManager;
import br.com.wdc.framework.persistence.transaction.TransactionScope;
import br.com.wdc.framework.persistence.transaction.TransactionServiceImpl;
import br.com.wdc.shopping.domain.ShoppingTransactions;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * Smoke test of the {@link TransactionService} (CMT style) in <b>JTA mode</b>, with an Agroal pool enlisted in the
 * Narayana TransactionManager over H2 in <b>XA</b> mode.
 *
 * <p>
 * Reproduces the production path of {@code SqlDataSourceSupport}/{@code JtaSupport} (which live in the backend module)
 * to exercise real demarcation: COMMIT on normal return, ROLLBACK on exception or {@code setRollbackOnly()}, and the
 * REQUIRES_NEW / MANDATORY / NEVER propagations.
 * </p>
 */
public class TransactionServiceJtaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:tx-smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static AgroalDataSource dataSource;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // 1) Narayana TransactionManager — ObjectStore under target/ so the cwd stays clean
        var objectStoreDir = new File("target/tx-smoke-store").getAbsolutePath();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());
        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());
        JtaTransactionManager.BEAN.set(com.arjuna.ats.jta.TransactionManager.transactionManager());

        // 2) H2 XA DataSource enlisted in the TM (mirrors the JTA branch of SqlDataSourceSupport)
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

        ShoppingTransactions.BEAN.set(new TransactionServiceImpl(() -> dataSource));

        // 3) Support table (outside any transaction)
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS TX_SMOKE (id INT PRIMARY KEY)");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        ShoppingTransactions.BEAN.set(null);
        JtaTransactionManager.BEAN.set(null);
    }

    private static TransactionService tx() {
        return ShoppingTransactions.BEAN.get();
    }

    /** Inserts using the current transaction's connection (takes part in the JTA scope). */
    private static void insert(int id) {
        try {
            var conn = TransactionScope.current().connection();
            try (var ps = conn.prepareStatement("INSERT INTO TX_SMOKE (id) VALUES (?)")) {
                ps.setInt(1, id);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Counts rows outside any transaction (sees only what was committed). */
    private static int count(int id) {
        try (var conn = dataSource.getConnection();
                var ps = conn.prepareStatement("SELECT COUNT(*) FROM TX_SMOKE WHERE id = ?")) {
            ps.setInt(1, id);
            try (var rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void required_normalReturn_commits() {
        tx().required(t -> insert(1));
        assertEquals("REQUIRED should commit on normal return", 1, count(1));
    }

    @Test
    public void required_exception_rollsBackAndPropagates() {
        try {
            tx().required(t -> {
                insert(2);
                throw new IllegalStateException("boom");
            });
            fail("the exception should have been rethrown");
        } catch (IllegalStateException expected) {
            // expected
        }
        assertEquals("REQUIRED should roll back when the work throws", 0, count(2));
    }

    @Test
    public void required_setRollbackOnly_rollsBack() {
        tx().required(t -> {
            insert(3);
            t.setRollbackOnly();
        });
        assertEquals("setRollbackOnly should roll back even on normal return", 0, count(3));
    }

    @Test
    public void requiresNew_commitsIndependently() {
        tx().requiresNew(t -> insert(4));
        assertEquals(1, count(4));
    }

    @Test
    public void mandatory_withoutActiveTx_throws() {
        try {
            tx().mandatory(t -> insert(5));
            fail("MANDATORY should require an active transaction");
        } catch (TransactionRequiredException expected) {
            // expected
        }
        assertEquals(0, count(5));
    }

    @Test
    public void never_withActiveTx_throws() {
        tx().required(outer -> {
            try {
                tx().never(inner -> {
                });
                fail("NEVER should forbid an active transaction");
            } catch (TransactionNotAllowedException expected) {
                // expected
            }
        });
    }
}
