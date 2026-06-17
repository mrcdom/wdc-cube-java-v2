package br.com.wdc.cube.backend.supports;

import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.jooq.SQLDialect;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.config.AppConfig;
import br.com.wdc.shopping.domain.ShoppingConfig;
import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;

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

        this.dialect = detectDialect(jdbcUrl);
        Class<?> driverClass = resolveDriverClass(dialect);

        int maxPoolSize = config.getInt(this.prefixDot + "database.pool.maxSize", 20);
        int minIdle = config.getInt(this.prefixDot + "database.pool.minIdle", 5);
        int connTimeout = config.getInt(this.prefixDot + "database.pool.connectionTimeoutSeconds", 30);

        var poolConfig = new AgroalDataSourceConfigurationSupplier()
                .connectionPoolConfiguration(new AgroalConnectionPoolConfigurationSupplier()
                        .maxSize(maxPoolSize)
                        .minSize(minIdle)
                        .initialSize(minIdle)
                        .acquisitionTimeout(Duration.ofSeconds(connTimeout))
                        .connectionFactoryConfiguration(new AgroalConnectionFactoryConfigurationSupplier()
                                .connectionProviderClass(driverClass)
                                .jdbcUrl(jdbcUrl)
                                .credential(new io.agroal.api.security.NamePrincipal(username))
                                .credential(new io.agroal.api.security.SimplePassword(password))));

        AgroalDataSource dataSource = AgroalDataSource.from(poolConfig);
        LOG.info(
                this.prefixDot
                        + "Connection pool configured: dialect={}, maxSize={}, minIdle={}, acquisitionTimeout={}s",
                dialect, maxPoolSize, minIdle, connTimeout);

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
     * Returns the DataSource / Driver provider class for Agroal based on the dialect. Agroal uses the provider class to
     * obtain connections — it supports both {@code javax.sql.DataSource} implementations and {@code java.sql.Driver}
     * subclasses.
     */
    private static Class<?> resolveDriverClass(SQLDialect dialect) {
        return switch (dialect) {
        case POSTGRES -> org.postgresql.ds.PGSimpleDataSource.class;
        default -> org.h2.jdbcx.JdbcDataSource.class;
        };
    }

}
