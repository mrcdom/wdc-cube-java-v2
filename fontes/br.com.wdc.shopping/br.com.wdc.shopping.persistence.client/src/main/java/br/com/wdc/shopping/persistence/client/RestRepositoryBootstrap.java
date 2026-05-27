package br.com.wdc.shopping.persistence.client;

import br.com.wdc.shopping.domain.codec.ProductModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseItemModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseModelCodec;
import br.com.wdc.shopping.domain.codec.UserModelCodec;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.CryptoProvider;
import br.com.wdc.shopping.domain.security.JceCryptoProvider;

/**
 * Inicializa os repositórios REST e registra nos BEANs estáticos do domínio.
 * <p>
 * Também registra o {@link AuthenticationService} para que o fluxo de login
 * via HMAC challenge-response funcione corretamente em clientes REST.
 */
public final class RestRepositoryBootstrap {

    private RestRepositoryBootstrap() {}

    public static void initialize(RestConfig config) {
        CryptoProvider.BEAN.set(new JceCryptoProvider());
        UserRepository.BEAN.set(new HttpUserRepository(config.transport(), new UserModelCodec()));
        ProductRepository.BEAN.set(new HttpProductRepository(config.transport(), new ProductModelCodec()));
        PurchaseRepository.BEAN.set(new HttpPurchaseRepository(config.transport(), new PurchaseModelCodec()));
        PurchaseItemRepository.BEAN.set(new HttpPurchaseItemRepository(config.transport(), new PurchaseItemModelCodec()));
        AuthenticationService.BEAN.set(new RestAuthenticationService(config));
    }

    public static void release() {
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }
}
