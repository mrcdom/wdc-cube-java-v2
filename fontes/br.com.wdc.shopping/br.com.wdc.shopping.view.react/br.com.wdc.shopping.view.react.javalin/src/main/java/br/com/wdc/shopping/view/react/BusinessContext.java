package br.com.wdc.shopping.view.react;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
            Path baseDir = resolveRuntimeBaseDir();
            Path configDir = createDirectory(baseDir.resolve("config"));
            Path dataDir = createDirectory(baseDir.resolve("data"));
            Path logDir = createDirectory(baseDir.resolve("log"));
            Path tempDir = createDirectory(baseDir.resolve("temp"));

            ShoppingConfig.Internals.setBaseDir(baseDir);
            ShoppingConfig.Internals.setConfigDir(configDir);
            ShoppingConfig.Internals.setDataDir(dataDir);
            ShoppingConfig.Internals.setLogDir(logDir);
            ShoppingConfig.Internals.setTempDir(tempDir);

            // Initialize centralized scheduled tasks manager with Virtual Thread executor
            var scheduledExecutor = createScheduledExecutor();

            // Service now use direct ReentrantReadWriteLock and execute synchronously on Virtual Threads
            ScheduledExecutor.BEAN.set(new ScheduledExecutorAdapter(scheduledExecutor));

            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setURL(resolveJdbcUrl(dataDir));
            dataSource.setUser(System.getProperty("wedocode.shopping.jdbc.username", "sa"));
            dataSource.setPassword(System.getProperty("wedocode.shopping.jdbc.password", "sa"));

            SqlDataSource.BEAN.set(new SqlDataSourceDelegate(dataSource));

            try (var connection = dataSource.getConnection()) {
                var command = new DBCreate().withConnection(connection);
                if (Boolean.getBoolean("wedocode.shopping.db.reset")) {
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

    private static String resolveJdbcUrl(Path dataDir) {
        String configuredUrl = System.getProperty("wedocode.shopping.jdbc.url");
        if (configuredUrl != null && !configuredUrl.isBlank()) {
            return configuredUrl;
        }
        var jdbcUrl = "jdbc:h2:file:" + dataDir.resolve(DEFAULT_DB_NAME).toAbsolutePath()
                + ";DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE";
        
        System.out.println(jdbcUrl);
        
        return jdbcUrl;
    }

    private static Path createDirectory(Path dir) throws IOException {
        Files.createDirectories(dir);
        return dir;
    }

    private static Path resolveRuntimeBaseDir() throws IOException {
        String configuredDir = System.getProperty("wedocode.shopping.runtime.dir");
        Path baseDir = configuredDir != null && !configuredDir.isBlank()
                ? Paths.get(configuredDir)
                : Paths.get("work");
        return createDirectory(baseDir.toAbsolutePath().normalize());
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
