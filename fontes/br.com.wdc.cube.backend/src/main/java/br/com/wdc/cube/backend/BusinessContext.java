package br.com.wdc.cube.backend;

import java.util.concurrent.ScheduledExecutorService;

import br.com.wdc.cube.backend.supports.JtaSupport;
import br.com.wdc.cube.backend.supports.SqlDataSourceSupport;
import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinatorImpl;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionOptions;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.persistence.impl.ShoppingRepositoryBootstrap;
import br.com.wdc.shopping.persistence.impl.concurrent.ScheduledExecutorAdapter;
import br.com.wdc.shopping.persistence.rest.RemoteTransactions;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.scripts.sgbd.DBCreate;
import br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap;

public class BusinessContext {

    public void configure(Defer cleanUp) {
        try {
            var config = ShoppingConfig.loadConfig();
            ShoppingConfig.Internals.configure(config);

            // CryptoProvider is required by PasswordUtil (used by the presentation layer on login)
            CryptoProvider.BEAN.set(new JceCryptoProvider());
            cleanUp.push(() -> CryptoProvider.BEAN.set(null));

            // Initialize centralized scheduled tasks manager with Virtual Thread executor
            var scheduledExecutor = createScheduledExecutor();

            // Service now use direct ReentrantReadWriteLock and execute synchronously on Virtual Threads
            ScheduledExecutor.BEAN.set(new ScheduledExecutorAdapter(scheduledExecutor));
            cleanUp.push(() -> {
                scheduledExecutor.close();
                ScheduledExecutor.BEAN.set(null);
            });

            var dbPrefix = "";

            // Transactions: initializes the Narayana TransactionManager if application.toml asks for it
            // (database.transaction = jta). Must come before the DataSource so the pool gets enlisted.
            var jtaSupport = new JtaSupport(dbPrefix, config);
            jtaSupport.init(cleanUp);

            var dsSupport = new SqlDataSourceSupport(dbPrefix, config);
            var dataSource = dsSupport.init(cleanUp);

            // Warns if JTA was requested but the TransactionManager did not start (running as plain JDBC).
            jtaSupport.warnIfModeMismatch();

            // The module's DataSource is injected into the bootstrap (no global holder): it binds this module's
            // DSLContext and TransactionService to this DataSource.
            ShoppingRepositoryBootstrap.initialize(dataSource, dsSupport.isLogEnabled(), dsSupport.getDialect(), cleanUp);

            // Schema: creates what is missing and seeds a new database; database.reset recreates everything.
            // After the bootstrap, because DBReset writes through the repositories.
            try (var connection = dataSource.getConnection()) {
                var command = new DBCreate()
                        .withConnection(connection);
                if (config.getBoolean(dbPrefix + "database.reset", false)) {
                    command.withReset();
                }
                command.run();
            }

            // Remote transaction coordinator (REST server): client-driven transactions over HTTP,
            // bound to this module's DataSource. It lives in the host (composition root), not in the shared bootstrap
            // — only the REST server needs it (local views/tests do not).
            RemoteTransactions.COORDINATOR.set(new RemoteTransactionCoordinatorImpl(() -> dataSource,
                    RemoteTransactionOptions.fromConfig(config, dbPrefix)));
            cleanUp.push(() -> RemoteTransactions.COORDINATOR.set(null));

            var jwtSecret = ShoppingConfig.getJwtSecret();
            if (jwtSecret != null && !jwtSecret.isBlank()) {
                ShoppingRepositoryBootstrap.initializeSecurity(jwtSecret, cleanUp);
            }

            LoginPresenter.simulateSlowLogin(config.getBoolean("simulation.slowLogin", false));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to configure shopping backend context", e);
        }
    }

    public void start(Defer cleanUp) {
        RemoteHostBootstrap.start(cleanUp);
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
