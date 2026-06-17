package br.com.wdc.cube.backend;

import java.util.concurrent.ScheduledExecutorService;

import br.com.wdc.cube.backend.supports.SqlDataSourceSupport;
import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.shopping.domain.ShoppingConfig;
import br.com.wdc.shopping.persistence.impl.RepositoryBootstrap;
import br.com.wdc.shopping.persistence.impl.concurrent.ScheduledExecutorAdapter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.view.remote.host.RemoteHostBootstrap;

public class BusinessContext {

    public void configure(Defer cleanUp) {
        try {
            var config = ShoppingConfig.loadConfig();
            ShoppingConfig.Internals.configure(config);

            // CryptoProvider é necessário para PasswordUtil (usado pela presentation layer no login)
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
            var dsSupport = new SqlDataSourceSupport(dbPrefix, config);
            SqlDataSource.BEAN.set(dsSupport.init(cleanUp));
            cleanUp.push(() -> SqlDataSource.BEAN.set(null));

            RepositoryBootstrap.initialize(dsSupport.isLogEnabled(), dsSupport.getDialect(), cleanUp);

            var jwtSecret = ShoppingConfig.getJwtSecret();
            if (jwtSecret != null && !jwtSecret.isBlank()) {
                RepositoryBootstrap.initializeSecurity(jwtSecret, cleanUp);
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
