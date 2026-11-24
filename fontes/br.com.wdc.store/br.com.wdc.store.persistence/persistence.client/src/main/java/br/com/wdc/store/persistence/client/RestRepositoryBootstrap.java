package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.CryptoProvider;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.domain.product.ProductCodec;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.PurchaseCodec;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserRepository;

/**
 * Initializes the REST repositories and registers them in the domain's static BEANs.
 * <p>
 * Also registers the {@link AuthenticationService} so that the HMAC
 * challenge-response login flow works correctly in REST clients.
 */
public final class RestRepositoryBootstrap {

    private RestRepositoryBootstrap() {}

    public static void initialize(HttpTransport transport, ClientStorage storage) {
        CryptoProvider.BEAN.set(new JceCryptoProvider());
        UserRepository.BEAN.set(new HttpUserRepository(transport, new UserCodec()));
        ProductRepository.BEAN.set(new HttpProductRepository(transport, new ProductCodec()));
        PurchaseRepository.BEAN.set(new HttpPurchaseRepository(transport, new PurchaseCodec()));
        PurchaseItemRepository.BEAN.set(new HttpPurchaseItemRepository(transport, new PurchaseItemCodec()));
        AuthenticationService.BEAN.set(new RestAuthenticationService(transport, storage));
        // Client TransactionService: coordinates the client-driven remote transaction (begin/commit/rollback + X-Tx-Id).
        StoreTransactions.BEAN.set(new RestTransactionService(transport));
    }

    public static void release() {
        StoreTransactions.BEAN.set(null);
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }
}
