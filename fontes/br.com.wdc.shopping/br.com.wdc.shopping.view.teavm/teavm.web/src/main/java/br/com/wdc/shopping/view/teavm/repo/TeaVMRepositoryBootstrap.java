package br.com.wdc.shopping.view.teavm.repo;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.ShoppingTransactions;
import br.com.wdc.shopping.domain.product.ProductCodec;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.domain.purchase.PurchaseCodec;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.UserCodec;
import br.com.wdc.shopping.domain.user.UserRepository;
import br.com.wdc.shopping.persistence.client.HttpProductRepository;
import br.com.wdc.shopping.persistence.client.HttpPurchaseItemRepository;
import br.com.wdc.shopping.persistence.client.HttpPurchaseRepository;
import br.com.wdc.shopping.persistence.client.HttpUserRepository;
import br.com.wdc.shopping.persistence.client.RestTransactionService;

/**
 * Bootstrap para TeaVM: registra repositórios usando os codecs unificados
 * (mesmos usados no JVM) nos BEANs estáticos do domínio.
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
        // TransactionService cliente: coordena transação remota dirigida pelo cliente (begin/commit/rollback + X-Tx-Id).
        ShoppingTransactions.BEAN.set(new RestTransactionService(transport));
    }

    public static void release() {
        ShoppingTransactions.BEAN.set(null);
        AuthenticationService.BEAN.set(null);
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

}
