package br.com.wdc.shopping.persistence;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.persistence.security.AuthenticationServiceImpl;
import br.com.wdc.shopping.persistence.security.SecuredProductRepository;
import br.com.wdc.shopping.persistence.security.SecuredPurchaseItemRepository;
import br.com.wdc.shopping.persistence.security.SecuredPurchaseRepository;
import br.com.wdc.shopping.persistence.security.SecuredUserRepository;

public class RepositoryBootstrap {

    private RepositoryBootstrap() {
        super();
    }

    /**
     * Inicializa repositórios sem segurança (para testes ou views locais).
     */
    public static void initialize() {
        UserRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.user.UserRepositoryImpl());
        ProductRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.product.ProductRepositoryImpl());
        PurchaseRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.purchase.PurchaseRepositoryImpl());
        PurchaseItemRepository.BEAN
                .set(new br.com.wdc.shopping.persistence.repository.purchaseitem.PurchaseItemRepositoryImpl());
    }

    /**
     * Ativa segurança: envolve os repositórios com decorators de permissão
     * e inicializa o {@link AuthenticationService}.
     * <p>
     * Deve ser chamado <b>após</b> {@link #initialize()}.
     *
     * @param jwtSecret segredo para assinatura JWT
     */
    public static void initializeSecurity(String jwtSecret) {
        // Guardar referência ao repo raw (auth service precisa para login)
        var rawUserRepo = UserRepository.BEAN.get();

        // Envolver todos os repos com decorators de segurança
        UserRepository.BEAN.set(new SecuredUserRepository(rawUserRepo));
        ProductRepository.BEAN.set(new SecuredProductRepository(ProductRepository.BEAN.get()));
        PurchaseRepository.BEAN.set(new SecuredPurchaseRepository(PurchaseRepository.BEAN.get()));
        PurchaseItemRepository.BEAN.set(new SecuredPurchaseItemRepository(PurchaseItemRepository.BEAN.get()));

        // Auth service usa o repo raw (bypass segurança para consultas de login)
        AuthenticationService.BEAN.set(new AuthenticationServiceImpl(rawUserRepo, jwtSecret));
    }

    public static void release() {
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

}
