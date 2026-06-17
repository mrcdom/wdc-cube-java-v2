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

import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.persistence.transaction.JtaTransactionManager;
import br.com.wdc.framework.persistence.transaction.TransactionScope;
import br.com.wdc.framework.persistence.transaction.TransactionServiceImpl;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * Teste de fumaça do {@link TransactionService} (estilo CMT) no <b>modo JTA</b>, com pool Agroal enlistado no
 * TransactionManager Narayana sobre H2 em modo <b>XA</b>.
 *
 * <p>
 * Reproduz o caminho de produção de {@code SqlDataSourceSupport}/{@code JtaSupport} (que vivem no módulo backend) para
 * exercitar a demarcação real: COMMIT em retorno normal, ROLLBACK em exceção ou {@code setRollbackOnly()}, e as
 * propagações REQUIRES_NEW / MANDATORY / NEVER.
 * </p>
 */
public class TransactionServiceJtaTest {

    private static final String JDBC_URL = "jdbc:h2:mem:tx-smoke;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";

    private static AgroalDataSource dataSource;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // 1) TransactionManager Narayana — ObjectStore em target/ para não poluir o cwd
        var objectStoreDir = new File("target/tx-smoke-store").getAbsolutePath();
        arjPropertyManager.getObjectStoreEnvironmentBean().setObjectStoreDir(objectStoreDir);
        arjPropertyManager.getObjectStoreEnvironmentBean().setLocalOSRoot("defaultStore");
        jtaPropertyManager.getJTAEnvironmentBean()
                .setTransactionManagerClassName(TransactionManagerImple.class.getName());
        jtaPropertyManager.getJTAEnvironmentBean()
                .setUserTransactionClassName(UserTransactionImple.class.getName());
        JtaTransactionManager.BEAN.set(com.arjuna.ats.jta.TransactionManager.transactionManager());

        // 2) DataSource H2 XA enlistado no TM (espelha o ramo JTA de SqlDataSourceSupport)
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

        SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));
        TransactionService.BEAN.set(new TransactionServiceImpl());

        // 3) Tabela de apoio (fora de transação)
        try (var conn = dataSource.getConnection(); var st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS TX_SMOKE (id INT PRIMARY KEY)");
        }
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        if (dataSource != null) {
            dataSource.close();
        }
        TransactionService.BEAN.set(null);
        SqlDataSource.BEAN.set(null);
        JtaTransactionManager.BEAN.set(null);
    }

    private static TransactionService tx() {
        return TransactionService.BEAN.get();
    }

    /** Insere usando a conexão da transação corrente (participa do escopo JTA). */
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

    /** Conta linhas fora de qualquer transação (vê apenas o que foi comitado). */
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
        assertEquals("REQUIRED deveria comitar em retorno normal", 1, count(1));
    }

    @Test
    public void required_exception_rollsBackAndPropagates() {
        try {
            tx().required(t -> {
                insert(2);
                throw new IllegalStateException("boom");
            });
            fail("a exceção deveria ter sido repropagada");
        } catch (IllegalStateException expected) {
            // esperado
        }
        assertEquals("REQUIRED deveria reverter quando o work lança", 0, count(2));
    }

    @Test
    public void required_setRollbackOnly_rollsBack() {
        tx().required(t -> {
            insert(3);
            t.setRollbackOnly();
        });
        assertEquals("setRollbackOnly deveria reverter mesmo em retorno normal", 0, count(3));
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
            fail("MANDATORY deveria exigir transação ativa");
        } catch (TransactionRequiredException expected) {
            // esperado
        }
        assertEquals(0, count(5));
    }

    @Test
    public void never_withActiveTx_throws() {
        tx().required(outer -> {
            try {
                tx().never(inner -> {
                });
                fail("NEVER deveria proibir transação ativa");
            } catch (TransactionNotAllowedException expected) {
                // esperado
            }
        });
    }
}
