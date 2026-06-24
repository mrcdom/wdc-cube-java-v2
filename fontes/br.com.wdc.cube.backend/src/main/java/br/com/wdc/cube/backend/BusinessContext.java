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

            // Transações: inicializa o TransactionManager Narayana se application.toml pedir
            // (database.transaction = jta). Deve preceder o DataSource para que o pool seja enlistado.
            var jtaSupport = new JtaSupport(dbPrefix, config);
            jtaSupport.init(cleanUp);

            var dsSupport = new SqlDataSourceSupport(dbPrefix, config);
            var dataSource = dsSupport.init(cleanUp);

            // Avisa se pediram JTA mas o TransactionManager não subiu (rodando como JDBC direto).
            jtaSupport.warnIfModeMismatch();

            // O DataSource do módulo é injetado no bootstrap (sem holder global): ele liga o DSLContext e o
            // TransactionService deste módulo a este DataSource.
            ShoppingRepositoryBootstrap.initialize(dataSource, dsSupport.isLogEnabled(), dsSupport.getDialect(), cleanUp);

            // Coordenador de transação remota (servidor REST): transações dirigidas pelo cliente sobre HTTP,
            // ligado ao DataSource deste módulo. Vive no host (composition root), não no bootstrap compartilhado
            // — só o servidor REST precisa dele (views locais/testes não).
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
