package br.com.wdc.cube.backend.supports;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.jooq.SQLDialect;

import com.arjuna.ats.jta.common.jtaPropertyManager;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.config.AppConfig;
import br.com.wdc.framework.persistence.transaction.JtaTransactionManager;
import br.com.wdc.shopping.domain.ShoppingConfig;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ExceptionSorter;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.narayana.NarayanaTransactionIntegration;

/**
 * Suporte de bootstrap do <b>DataSource</b> (pool Agroal) para o host.
 *
 * <p>
 * Detecta o dialeto (H2/PostgreSQL) a partir da URL e configura o pool com validação de conexões idle (resiliência
 * contra sockets derrubados por firewall). Quando o modo JTA está ativo ({@link JtaTransactionManager#BEAN}
 * inicializado), enlista o pool no TransactionManager Narayana via {@link NarayanaTransactionIntegration} e usa o driver
 * XA do banco — caso contrário opera em JDBC direto.
 * </p>
 *
 * <p>
 * Toda a tecnologia concreta (Agroal, Narayana, drivers XA) vive aqui no host; o módulo {@code framework.persistence}
 * permanece neutro, consumindo apenas {@code javax.sql.DataSource} e {@code jakarta.transaction.TransactionManager}.
 * </p>
 */
public class SqlDataSourceSupport {

    private static final Log LOG = Log.getLogger(SqlDataSourceSupport.class);

    private static final String DEFAULT_DB_NAME = "wedocode-shopping";

    private AppConfig config;
    private String prefixDot;
    private SQLDialect dialect;

    public SqlDataSourceSupport(String prefix, AppConfig config) {
        this.config = config;
        this.prefixDot = StringUtils.isEmpty(prefix) ? "" : prefix + ".";
    }

    public SQLDialect getDialect() {
        return this.dialect;
    }

    public boolean isLogEnabled() {
        return config.getBoolean(prefixDot + "database.logSql", false);
    }

    public SqlDataSourceDelegate init(Defer cleanUp) throws SQLException {
        String jdbcUrl = resolveJdbcUrl(config, ShoppingConfig.getDataDir());
        String username = config.get(this.prefixDot + "database.username", "sa");
        String password = config.get(this.prefixDot + "database.password", "sa");
        String schema = config.get(this.prefixDot + "database.schema", null);

        this.dialect = detectDialect(jdbcUrl);
        boolean jtaEnabled = JtaTransactionManager.BEAN.get() != null;
        Class<?> providerClass = resolveProviderClass(dialect, jtaEnabled);

        int maxPoolSize = config.getInt(this.prefixDot + "database.pool.maxSize", 20);
        int minIdle = config.getInt(this.prefixDot + "database.pool.minIdle", 5);
        int connTimeout = config.getInt(this.prefixDot + "database.pool.connectionTimeoutSeconds", 30);
        int maxLifetimeMin = config.getInt(this.prefixDot + "database.pool.maxLifetimeMinutes", 30);

        var factoryConfig = new AgroalConnectionFactoryConfigurationSupplier()
                .connectionProviderClass(providerClass)
                .credential(new NamePrincipal(username))
                .credential(new SimplePassword(password));
        // XADataSource recebe a URL via propriedade de bean; DataSource/Driver comum via jdbcUrl.
        if (jtaEnabled) {
            factoryConfig.jdbcProperty("url", jdbcUrl);
        } else {
            factoryConfig.jdbcUrl(jdbcUrl);
        }
        // PostgreSQL com schema dedicado: posiciona o search_path em cada conexão nova.
        if (dialect == SQLDialect.POSTGRES && StringUtils.isNotBlank(schema)) {
            factoryConfig.initialSql("SET search_path TO " + schema);
        }

        var poolConfig = new AgroalConnectionPoolConfigurationSupplier()
                .maxSize(maxPoolSize)
                .minSize(minIdle)
                .initialSize(minIdle)
                .acquisitionTimeout(Duration.ofSeconds(connTimeout))
                // Resiliência: firewall/banco podem derrubar conexões idle silenciosamente. Sem isso o pool
                // re-entrega sockets mortos. Valida após inatividade, recicla por idade e descarta erro fatal.
                .connectionValidator(ConnectionValidator.defaultValidator())
                .idleValidationTimeout(Duration.ofSeconds(60))
                .reapTimeout(Duration.ofMinutes(10))
                .exceptionSorter(ExceptionSorter.defaultExceptionSorter())
                .connectionFactoryConfiguration(factoryConfig);
        if (maxLifetimeMin > 0) {
            poolConfig.maxLifetime(Duration.ofMinutes(maxLifetimeMin));
        }
        // Modo JTA: enlista o pool no TransactionManager (2PC). A tecnologia concreta fica contida aqui.
        if (jtaEnabled) {
            var tsr = jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();
            poolConfig.transactionIntegration(new NarayanaTransactionIntegration(JtaTransactionManager.get(), tsr));
        }

        var dsConfig = new AgroalDataSourceConfigurationSupplier().connectionPoolConfiguration(poolConfig);

        AgroalDataSource dataSource = AgroalDataSource.from(dsConfig);
        cleanUp.push(() -> {
            try {
                dataSource.close();
                LOG.info(this.prefixDot + "DataSource encerrado.");
            } catch (Exception e) {
                LOG.warn("Falha ao encerrar AgroalDataSource", e);
            }
        });

        // PostgreSQL + JTA exige max_prepared_transactions > 0 no servidor para 2PC.
        if (jtaEnabled && dialect == SQLDialect.POSTGRES) {
            validateXaConfig(dataSource);
        }

        LOG.info(this.prefixDot + "Connection pool configured: dialect={}, jta={}, maxSize={}, minIdle={}",
                dialect, jtaEnabled, maxPoolSize, minIdle);
        LOG.info(this.prefixDot + "jdbc configured with database {}", jdbcUrl);

        return new SqlDataSourceDelegate(dataSource);
    }

    private static String resolveJdbcUrl(AppConfig config, Path dataDir) {
        String configuredUrl = config.get("database.url");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return "jdbc:h2:file:" + dataDir.resolve(DEFAULT_DB_NAME).toAbsolutePath()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    }

    private static SQLDialect detectDialect(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:postgresql:") || jdbcUrl.startsWith("jdbc:pgsql:")) {
            return SQLDialect.POSTGRES;
        }
        return SQLDialect.H2;
    }

    /**
     * Classe provedora de conexões para o Agroal. No modo JTA precisa ser um {@code XADataSource} (2PC):
     * {@code PGXADataSource} no PostgreSQL; o {@code JdbcDataSource} do H2 já é XA e serve aos dois modos.
     */
    private static Class<?> resolveProviderClass(SQLDialect dialect, boolean jtaEnabled) {
        return switch (dialect) {
        case POSTGRES -> jtaEnabled
                ? org.postgresql.xa.PGXADataSource.class
                : org.postgresql.ds.PGSimpleDataSource.class;
        default -> org.h2.jdbcx.JdbcDataSource.class;
        };
    }

    /**
     * Verifica se o PostgreSQL está configurado para transações XA (2PC). Falha rápido na subida se
     * {@code max_prepared_transactions = 0}.
     */
    private void validateXaConfig(AgroalDataSource ds) {
        try (var conn = ds.getConnection();
                var stmt = conn.createStatement();
                var rs = stmt.executeQuery("SHOW max_prepared_transactions")) {
            if (rs.next()) {
                int value = Integer.parseInt(rs.getString(1).trim());
                if (value == 0) {
                    throw new IllegalStateException(
                            "PostgreSQL não está configurado para transações XA: max_prepared_transactions = 0. "
                                    + "Defina max_prepared_transactions >= pool.maxSize em postgresql.conf e reinicie.");
                }
                LOG.info(this.prefixDot + "validateXaConfig: max_prepared_transactions = {}", value);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao verificar max_prepared_transactions no PostgreSQL", e);
        }
    }

}
