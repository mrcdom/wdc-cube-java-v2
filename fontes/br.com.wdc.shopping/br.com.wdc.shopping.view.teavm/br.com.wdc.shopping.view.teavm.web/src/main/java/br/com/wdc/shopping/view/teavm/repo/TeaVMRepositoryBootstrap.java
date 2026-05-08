package br.com.wdc.shopping.view.teavm.repo;

import br.com.wdc.shopping.api.client.HttpTransport;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;

/**
 * Bootstrap para TeaVM: registra repositórios que usam parsing manual de JSON
 * (sem Gson reflection) nos BEANs estáticos do domínio.
 */
public final class TeaVMRepositoryBootstrap {

    private TeaVMRepositoryBootstrap() {
    }

    public static void initialize(HttpTransport transport) {
        UserRepository.BEAN.set(new TeaVMUserRepository(transport));
        ProductRepository.BEAN.set(new TeaVMProductRepository(transport));
        PurchaseRepository.BEAN.set(new TeaVMPurchaseRepository(transport));
        PurchaseItemRepository.BEAN.set(new TeaVMPurchaseItemRepository(transport));
        AuthenticationService.BEAN.set(new TeaVMAuthenticationService(transport));
    }

    public static void release() {
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

}
