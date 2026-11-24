package br.com.wdc.store.view.teavm.repo;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.domain.product.ProductCodec;
import br.com.wdc.store.domain.product.ProductRepository;
import br.com.wdc.store.domain.purchase.PurchaseCodec;
import br.com.wdc.store.domain.purchase.PurchaseRepository;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.client.HttpProductRepository;
import br.com.wdc.store.persistence.client.HttpPurchaseItemRepository;
import br.com.wdc.store.persistence.client.HttpPurchaseRepository;
import br.com.wdc.store.persistence.client.HttpUserRepository;
import br.com.wdc.store.persistence.client.RestTransactionService;

/**
 * TeaVM bootstrap: registers repositories using the unified codecs
 * (the same ones used on the JVM) in the domain's static BEANs.
 */
public final class TeaVMRepositoryBootstrap {

    private TeaVMRepositoryBootstrap() {
    }

    public static void initialize(HttpTransport transport, ClientStorage storage) {
        UserRepository.BEAN.set(new HttpUserRepository(transport, new UserCodec()));
        ProductRepository.BEAN.set(new HttpProductRepository(transport, new ProductCodec()));
        PurchaseRepository.BEAN.set(new HttpPurchaseRepository(transport, new PurchaseCodec()));
        PurchaseItemRepository.BEAN.set(new HttpPurchaseItemRepository(transport, new PurchaseItemCodec()));
        AuthenticationService.BEAN.set(new TeaVMAuthenticationService(transport, storage));
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
