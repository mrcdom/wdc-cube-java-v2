package br.com.wdc.shopping.persistence;

import org.jooq.SQLDialect;
import org.jooq.conf.Settings;
import org.jooq.impl.DSL;

import br.com.wdc.framework.commons.sql.SqlDataSource;
import br.com.wdc.framework.jooq.JooqDSLContext;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.persistence.security.AuthenticationServiceImpl;

public class RepositoryBootstrap {

    private RepositoryBootstrap() {
        super();
    }

    /**
     * Inicializa repositórios sem segurança (para testes ou views locais).
     */
    public static void initialize() {
        initialize(false);
    }

    /**
     * Inicializa repositórios sem segurança, com opção de log de SQL.
     *
     * @param logSql se {@code true}, jOOQ loga os SQLs executados via SLF4J (nível DEBUG)
     */
    public static void initialize(boolean logSql) {
        var settings = new Settings().withExecuteLogging(logSql);
        JooqDSLContext.BEAN.set(DSL.using(SqlDataSource.BEAN.get(), SQLDialect.H2, settings));

        UserRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.UserRepositoryImpl());
        ProductRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.ProductRepositoryImpl());
        PurchaseRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.PurchaseRepositoryImpl());
        PurchaseItemRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.PurchaseItemRepositoryImpl());
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
    public static void initializeSecurity(String jwtSecret) {
        var rawUserRepo = UserRepository.BEAN.get();
        AuthenticationService.BEAN.set(new AuthenticationServiceImpl(rawUserRepo, jwtSecret));
    }

    public static void release() {
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
        JooqDSLContext.BEAN.set(null);
    }

}
