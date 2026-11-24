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
import br.com.wdc.store.domain.StoreConfig;
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
 * Bootstrap support for the host's <b>DataSource</b> (Agroal pool).
 *
 * <p>
 * Detects the dialect (H2/PostgreSQL) from the URL and configures the pool with idle-connection validation (resilience
 * against sockets dropped by a firewall). When JTA mode is active ({@link JtaTransactionManager#BEAN} initialized), it
 * enlists the pool in the Narayana TransactionManager via {@link NarayanaTransactionIntegration} and uses the
 * database's XA driver — otherwise it runs as plain JDBC.
 * </p>
 *
 * <p>
 * All the concrete technology (Agroal, Narayana, XA drivers) lives here in the host; the {@code framework.persistence}
 * module stays neutral, consuming only {@code javax.sql.DataSource} and {@code jakarta.transaction.TransactionManager}.
 * </p>
 */
public class SqlDataSourceSupport {

    private static final Log LOG = Log.getLogger(SqlDataSourceSupport.class);

    private static final String DEFAULT_DB_NAME = "wedocode-store";

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
        String jdbcUrl = resolveJdbcUrl(config, StoreConfig.getDataDir());
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
        // XADataSource receives the URL through a bean property; a regular DataSource/Driver through jdbcUrl.
        if (jtaEnabled) {
            factoryConfig.jdbcProperty("url", jdbcUrl);
        } else {
            factoryConfig.jdbcUrl(jdbcUrl);
        }
        // PostgreSQL with a dedicated schema: sets the search_path on each new connection.
        if (dialect == SQLDialect.POSTGRES && StringUtils.isNotBlank(schema)) {
            factoryConfig.initialSql("SET search_path TO " + schema);
        }

        var poolConfig = new AgroalConnectionPoolConfigurationSupplier()
                .maxSize(maxPoolSize)
                .minSize(minIdle)
                .initialSize(minIdle)
                .acquisitionTimeout(Duration.ofSeconds(connTimeout))
                // Resilience: the firewall/database may silently drop idle connections. Without this the pool
                // hands out dead sockets again. Validates after inactivity, recycles by age and discards on fatal error.
                .connectionValidator(ConnectionValidator.defaultValidator())
                .idleValidationTimeout(Duration.ofSeconds(60))
                .reapTimeout(Duration.ofMinutes(10))
                .exceptionSorter(ExceptionSorter.defaultExceptionSorter())
                .connectionFactoryConfiguration(factoryConfig);
        if (maxLifetimeMin > 0) {
            poolConfig.maxLifetime(Duration.ofMinutes(maxLifetimeMin));
        }
        // JTA mode: enlists the pool in the TransactionManager (2PC). The concrete technology stays contained here.
        if (jtaEnabled) {
            var tsr = jtaPropertyManager.getJTAEnvironmentBean().getTransactionSynchronizationRegistry();
            poolConfig.transactionIntegration(new NarayanaTransactionIntegration(JtaTransactionManager.get(), tsr));
        }

        var dsConfig = new AgroalDataSourceConfigurationSupplier().connectionPoolConfiguration(poolConfig);

        AgroalDataSource dataSource = AgroalDataSource.from(dsConfig);
        cleanUp.push(() -> {
            try {
                dataSource.close();
                LOG.info(this.prefixDot + "DataSource closed.");
            } catch (Exception e) {
                LOG.warn("Failed to close AgroalDataSource", e);
            }
        });

        // PostgreSQL + JTA requires max_prepared_transactions > 0 on the server for 2PC.
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
     * Connection provider class for Agroal. In JTA mode it must be an {@code XADataSource} (2PC):
     * {@code PGXADataSource} on PostgreSQL; H2's {@code JdbcDataSource} is already XA and serves both modes.
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
     * Checks whether PostgreSQL is configured for XA (2PC) transactions. Fails fast at startup if
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
                            "PostgreSQL is not configured for XA transactions: max_prepared_transactions = 0. "
                                    + "Set max_prepared_transactions >= pool.maxSize in postgresql.conf and restart.");
                }
                LOG.info(this.prefixDot + "validateXaConfig: max_prepared_transactions = {}", value);
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to check max_prepared_transactions on PostgreSQL", e);
        }
    }

}
