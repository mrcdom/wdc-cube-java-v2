package br.com.wdc.shopping.persistence;

import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.repositories.UserRepository;

public class RepositoryBootstrap {

    private RepositoryBootstrap() {
        super();
    }

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

    public static void release() {
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

}
