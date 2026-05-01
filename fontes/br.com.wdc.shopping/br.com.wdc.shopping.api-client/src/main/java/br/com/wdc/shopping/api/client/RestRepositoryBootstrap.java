package br.com.wdc.shopping.api.client;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;

/**
 * Inicializa os repositórios REST e registra nos BEANs estáticos do domínio.
 */
public final class RestRepositoryBootstrap {

    private RestRepositoryBootstrap() {}

    public static void initialize(RestConfig config) {
        UserRepository.BEAN.set(new RestUserRepository(config));
        ProductRepository.BEAN.set(new RestProductRepository(config));
        PurchaseRepository.BEAN.set(new RestPurchaseRepository(config));
        PurchaseItemRepository.BEAN.set(new RestPurchaseItemRepository(config));
    }

    public static void release() {
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }
}
