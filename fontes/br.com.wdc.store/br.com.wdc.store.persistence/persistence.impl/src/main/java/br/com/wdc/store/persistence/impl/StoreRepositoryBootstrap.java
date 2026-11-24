package br.com.wdc.store.persistence.impl;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.jooq.TransactionAwareConnectionProvider;
import br.com.wdc.framework.persistence.transaction.TransactionServiceImpl;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.impl.repository.ProductRepositoryImpl;
import br.com.wdc.store.persistence.impl.repository.PurchaseItemRepositoryImpl;
import br.com.wdc.store.persistence.impl.repository.PurchaseRepositoryImpl;
import br.com.wdc.store.persistence.impl.repository.UserRepositoryImpl;
import br.com.wdc.store.persistence.impl.security.AuthenticationServiceImpl;

public class StoreRepositoryBootstrap {

    private StoreRepositoryBootstrap() {
        super();
    }

    /**
     * Initializes repositories without security (for tests or local views), over the module's {@code dataSource}.
     */
    public static void initialize(DataSource dataSource, Defer cleanUp) {
        initialize(dataSource, false, cleanUp);
    }

    /**
     * Initializes repositories without security, with optional SQL logging.
     *
     * @param dataSource DataSource of <b>this module</b> (injected by the composition root)
     * @param logSql     if {@code true}, jOOQ logs the executed SQL via SLF4J (DEBUG level)
     */
    public static void initialize(DataSource dataSource, boolean logSql, Defer cleanUp) {
        initialize(dataSource, logSql, SQLDialect.H2, cleanUp);
    }

    /**
     * Initializes repositories without security, with an explicit dialect.
     *
     * @param dataSource DataSource of <b>this module</b> — used both by the {@code DSLContext} and by the
     *                   module's {@code TransactionService}
     * @param logSql     if {@code true}, jOOQ logs the executed SQL via SLF4J (DEBUG level)
     * @param dialect    SQL dialect to use (e.g. {@link SQLDialect#H2}, {@link SQLDialect#POSTGRES})
     */
    public static void initialize(DataSource dataSource, boolean logSql, SQLDialect dialect, Defer cleanUp) {
        var settings = new Settings()
                .withExecuteLogging(logSql);
        if (dialect == SQLDialect.POSTGRES) {
            // The jOOQ-generated classes reference schema "PUBLIC" (from H2 codegen).
            // PostgreSQL treats quoted identifiers as case-sensitive, so "PUBLIC" != "public".
            // Disabling schema rendering removes the prefix entirely; PostgreSQL resolves
            // tables through search_path (default: public).
            settings = settings.withRenderSchema(false);
            // H2 codegen produces uppercase names ("EN_USER"). PostgreSQL folds unquoted DDL
            // identifiers to lowercase (en_user). Rendering in lowercase makes jOOQ emit
            // "en_user" which matches the actual stored name.
            settings = settings.withRenderNameCase(RenderNameCase.LOWER);
        }
        // TransactionScope-aware DSLContext: inside a transaction, queries use the scope's connection
        // (same physical transaction); outside it, a standalone pool connection (autocommit). Applies to JDBC and JTA.
        var connectionProvider = new TransactionAwareConnectionProvider(dataSource);
        StoreDSLContext.BEAN.set(DSL.using(connectionProvider, dialect, settings));

        UserRepository.BEAN.set(new UserRepositoryImpl());
        ProductRepository.BEAN.set(new ProductRepositoryImpl());
        PurchaseRepository.BEAN.set(new PurchaseRepositoryImpl());
        PurchaseItemRepository.BEAN.set(new PurchaseItemRepositoryImpl());

        // The module's TransactionService, bound to this module's DataSource (not to a shared global one).
        StoreTransactions.BEAN.set(new TransactionServiceImpl(() -> dataSource));

        cleanUp.push(() -> {
            StoreTransactions.BEAN.set(null);
            StoreDSLContext.BEAN.set(null);
            UserRepository.BEAN.set(null);
            ProductRepository.BEAN.set(null);
            PurchaseRepository.BEAN.set(null);
            PurchaseItemRepository.BEAN.set(null);
        });
    }

    /**
     * Enables security: initializes the {@link AuthenticationService} for JWT authentication.
     * <p>
     * Permission and scope checks are done directly in the REST endpoints (ApiControllers).
     * <p>
     * Must be called <b>after</b> {@link #initialize()}.
     *
     * @param jwtSecret secret for JWT signing
     */
    public static void initializeSecurity(String jwtSecret, Defer cleanUp) {
        var rawUserRepo = UserRepository.BEAN.get();
        AuthenticationService.BEAN.set(new AuthenticationServiceImpl(rawUserRepo, jwtSecret));
        cleanUp.push(() -> AuthenticationService.BEAN.set(null));
    }

}
