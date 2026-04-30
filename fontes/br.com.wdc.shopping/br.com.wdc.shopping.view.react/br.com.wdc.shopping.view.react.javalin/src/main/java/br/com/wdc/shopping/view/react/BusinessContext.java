package br.com.wdc.shopping.view.react;

import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.sql.SqlDataSourceDelegate;
import br.com.wdc.shopping.business.impl.RepositoryBootstrap;
import br.com.wdc.shopping.business.impl.concurrent.ScheduledExecutorAdapter;
import br.com.wdc.shopping.business.impl.sgbd.ddl.scripts.DBCreate;
import br.com.wdc.shopping.business.shared.ShoppingConfig;
import br.com.wdc.shopping.business.shared.config.AppConfig;

public class BusinessContext {

    private static final Logger LOG = LoggerFactory.getLogger(BusinessContext.class);

    private static final String DEFAULT_DB_NAME = "wedocode-shopping";

    public void stop() {
        RepositoryBootstrap.release();
        ScheduledExecutor.BEAN.set(null);
        SqlDataSource.BEAN.set(null);
    }

    public void start() {
        try {
            var config = AppConfig.load();
            ShoppingConfig.Internals.configure(config);

            // Initialize centralized scheduled tasks manager with Virtual Thread executor
            var scheduledExecutor = createScheduledExecutor();

            // Service now use direct ReentrantReadWriteLock and execute synchronously on Virtual Threads
            ScheduledExecutor.BEAN.set(new ScheduledExecutorAdapter(scheduledExecutor));

            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(resolveJdbcUrl(config, ShoppingConfig.getDataDir()));
            dataSource.setUser(config.get("database.username", "sa"));
            dataSource.setPassword(config.get("database.password", "sa"));

            SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

            try (var connection = dataSource.getConnection()) {
                var command = new DBCreate().withConnection(connection);
                if (config.getBoolean("database.reset", false)) {
                    command.withReset();
                }
                command.run();
            }

            RepositoryBootstrap.initialize();

            LOG.info("Shopping backend context initialized with database {}", dataSource.getURL());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize shopping backend context", e);
        }
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
