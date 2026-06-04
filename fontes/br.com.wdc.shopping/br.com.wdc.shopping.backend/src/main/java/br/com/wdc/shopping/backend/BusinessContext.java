package br.com.wdc.shopping.backend;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.jdbcx.JdbcDataSource;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.domain.config.AppConfig;
import br.com.wdc.shopping.domain.security.CryptoProvider;
import br.com.wdc.shopping.domain.security.JceCryptoProvider;
import br.com.wdc.shopping.persistence.RepositoryBootstrap;
import br.com.wdc.shopping.persistence.concurrent.ScheduledExecutorAdapter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap;

public class BusinessContext {

    private static final Log LOG = Log.getLogger(BusinessContext.class);

    private static final String DEFAULT_DB_NAME = "wedocode-shopping";

    public void stop() {
        RemoteHostBootstrap.stop();
        RepositoryBootstrap.release();
        ScheduledExecutor.BEAN.set(null);
        SqlDataSource.BEAN.set(null);
        CryptoProvider.BEAN.set(null);
    }

    public void configure() {
        try {
            var config = AppConfig.load();
            ShoppingConfig.Internals.configure(config);

            // CryptoProvider é necessário para PasswordUtil (usado pela presentation layer no login)
            CryptoProvider.BEAN.set(new JceCryptoProvider());

            // Initialize centralized scheduled tasks manager with Virtual Thread executor
            var scheduledExecutor = createScheduledExecutor();

            // Service now use direct ReentrantReadWriteLock and execute synchronously on Virtual Threads
            ScheduledExecutor.BEAN.set(new ScheduledExecutorAdapter(scheduledExecutor));

            var xaDataSource = new JdbcDataSource();
            xaDataSource.setURL(resolveJdbcUrl(config, ShoppingConfig.getDataDir()));
            xaDataSource.setUser(config.get("database.username", "sa"));
            xaDataSource.setPassword(config.get("database.password", "sa"));

            int maxPoolSize  = config.getInt("database.pool.maxSize", 20);
            int minIdle      = config.getInt("database.pool.minIdle", 5);
            int connTimeout  = config.getInt("database.pool.connectionTimeoutSeconds", 30);

            var poolConfig = new AgroalDataSourceConfigurationSupplier()
                    .connectionPoolConfiguration(new AgroalConnectionPoolConfigurationSupplier()
                            .maxSize(maxPoolSize)
                            .minSize(minIdle)
                            .initialSize(minIdle)
                            .acquisitionTimeout(Duration.ofSeconds(connTimeout))
                            .connectionFactoryConfiguration(new AgroalConnectionFactoryConfigurationSupplier()
                                    .connectionProviderClass(org.h2.jdbcx.JdbcDataSource.class)
                                    .jdbcUrl(xaDataSource.getURL())
                                    .credential(new io.agroal.api.security.NamePrincipal(xaDataSource.getUser()))
                                    .credential(new io.agroal.api.security.SimplePassword(xaDataSource.getPassword()))));

            AgroalDataSource dataSource = AgroalDataSource.from(poolConfig);
            LOG.info("Connection pool configured: maxSize={}, minIdle={}, acquisitionTimeout={}s",
                    maxPoolSize, minIdle, connTimeout);

            SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

            RepositoryBootstrap.initialize(config.getBoolean("database.logSql", false));

            try (var connection = dataSource.getConnection()) {
                var command = new DBCreate().withConnection(connection);
                if (config.getBoolean("database.reset", false)) {
                    command.withReset();
                }
                command.run();
            }

            var jwtSecret = ShoppingConfig.getJwtSecret();
            if (jwtSecret != null && !jwtSecret.isBlank()) {
                RepositoryBootstrap.initializeSecurity(jwtSecret);
            }

            LoginPresenter.simulateSlowLogin(config.getBoolean("simulation.slowLogin", false));

            LOG.info("Shopping backend context configured with database {}", xaDataSource.getURL());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure shopping backend context", e);
        }
    }

    public void start() {
        RemoteHostBootstrap.start();
    }

    private static String resolveJdbcUrl(AppConfig config, Path dataDir) {
        String configuredUrl = config.get("database.url");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        return "jdbc:h2:file:" + dataDir.resolve(DEFAULT_DB_NAME).toAbsolutePath()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
    }

    /**
     * Creates a Virtual Thread-based executor service for scheduled tasks.
     * 
     * This executor is used by ScheduledTasksManager for all background tasks. Virtual Threads are ideal for: -
     * I/O-bound operations (session cleanup, monitoring) - Infrequent scheduled work that doesn't require OS thread
     * preservation - Applications with many concurrent tasks
     * 
     * @return a new ScheduledExecutorService using Virtual Thread factory
     */
    private ScheduledExecutorService createScheduledExecutor() {
        return java.util.concurrent.Executors
                .newScheduledThreadPool(1, VirtualThreadFactory.ofVirtual("ScheduledTasks"));
    }
}
