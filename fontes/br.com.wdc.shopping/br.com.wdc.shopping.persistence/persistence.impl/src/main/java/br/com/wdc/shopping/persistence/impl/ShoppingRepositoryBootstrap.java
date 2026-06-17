package br.com.wdc.shopping.persistence.impl;

import javax.sql.DataSource;

import org.jooq.SQLDialect;
import org.jooq.conf.RenderNameCase;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import br.com.wdc.framework.commons.util.Defer;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.jooq.TransactionAwareConnectionProvider;
import br.com.wdc.framework.persistence.transaction.TransactionServiceImpl;
import br.com.wdc.shopping.domain.ShoppingTransactions;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.persistence.impl.repository.ProductRepositoryImpl;
import br.com.wdc.shopping.persistence.impl.repository.PurchaseItemRepositoryImpl;
import br.com.wdc.shopping.persistence.impl.repository.PurchaseRepositoryImpl;
import br.com.wdc.shopping.persistence.impl.repository.UserRepositoryImpl;
import br.com.wdc.shopping.persistence.impl.security.AuthenticationServiceImpl;

public class ShoppingRepositoryBootstrap {

    private ShoppingRepositoryBootstrap() {
        super();
    }

    /**
     * Inicializa repositórios sem segurança (para testes ou views locais), sobre o {@code dataSource} do módulo.
     */
    public static void initialize(DataSource dataSource, Defer cleanUp) {
        initialize(dataSource, false, cleanUp);
    }

    /**
     * Inicializa repositórios sem segurança, com opção de log de SQL.
     *
     * @param dataSource DataSource <b>deste módulo</b> (injetado pelo composition root)
     * @param logSql     se {@code true}, jOOQ loga os SQLs executados via SLF4J (nível DEBUG)
     */
    public static void initialize(DataSource dataSource, boolean logSql, Defer cleanUp) {
        initialize(dataSource, logSql, SQLDialect.H2, cleanUp);
    }

    /**
     * Inicializa repositórios sem segurança, com dialeto explícito.
     *
     * @param dataSource DataSource <b>deste módulo</b> — usado tanto pelo {@code DSLContext} quanto pelo
     *                   {@code TransactionService} do módulo
     * @param logSql     se {@code true}, jOOQ loga os SQLs executados via SLF4J (nível DEBUG)
     * @param dialect    dialeto SQL a usar (ex: {@link SQLDialect#H2}, {@link SQLDialect#POSTGRES})
     */
    public static void initialize(DataSource dataSource, boolean logSql, SQLDialect dialect, Defer cleanUp) {
        var settings = new Settings().withExecuteLogging(logSql);
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
        // DSLContext ciente do TransactionScope: dentro de uma transação as queries usam a conexão do escopo
        // (mesma transação física); fora dela, conexão avulsa do pool (autocommit). Vale para JDBC e JTA.
        var connectionProvider = new TransactionAwareConnectionProvider(dataSource);
        ShoppingDSLContext.BEAN.set(DSL.using(connectionProvider, dialect, settings));

        UserRepository.BEAN.set(new UserRepositoryImpl());
        ProductRepository.BEAN.set(new ProductRepositoryImpl());
        PurchaseRepository.BEAN.set(new PurchaseRepositoryImpl());
        PurchaseItemRepository.BEAN.set(new PurchaseItemRepositoryImpl());

        // TransactionService do módulo, ligado ao DataSource deste módulo (não a um global compartilhado).
        ShoppingTransactions.BEAN.set(new TransactionServiceImpl(() -> dataSource));

        cleanUp.push(() -> {
            ShoppingTransactions.BEAN.set(null);
            ShoppingDSLContext.BEAN.set(null);
            UserRepository.BEAN.set(null);
            ProductRepository.BEAN.set(null);
            PurchaseRepository.BEAN.set(null);
            PurchaseItemRepository.BEAN.set(null);
        });
    }

    /**
     * Ativa segurança: inicializa o {@link AuthenticationService} para autenticação JWT.
     * <p>
     * A verificação de permissões e escopo é feita diretamente nos endpoints REST (ApiControllers).
     * <p>
     * Deve ser chamado <b>após</b> {@link #initialize()}.
     *
     * @param jwtSecret segredo para assinatura JWT
     */
    public static void initializeSecurity(String jwtSecret, Defer cleanUp) {
        var rawUserRepo = UserRepository.BEAN.get();
        AuthenticationService.BEAN.set(new AuthenticationServiceImpl(rawUserRepo, jwtSecret));
        cleanUp.push(() -> AuthenticationService.BEAN.set(null));
    }

}
