package br.com.wdc.shopping.business.impl;

import br.com.wdc.shopping.business.shared.repositories.ProductRepository;
import br.com.wdc.shopping.business.shared.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.business.shared.repositories.PurchaseRepository;
import br.com.wdc.shopping.business.shared.repositories.UserRepository;

public class RepositoryBootstrap {

    private RepositoryBootstrap() {
        super();
    }

    public static void initialize() {
        UserRepository.BEAN
                .set(new br.com.wdc.shopping.business.impl.sgbd.repository.user.UserRepositoryImpl());
        ProductRepository.BEAN
                .set(new br.com.wdc.shopping.business.impl.sgbd.repository.product.ProductRepositoryImpl());
        PurchaseRepository.BEAN
                .set(new br.com.wdc.shopping.business.impl.sgbd.repository.purchase.PurchaseRepositoryImpl());
        PurchaseItemRepository.BEAN
                .set(new br.com.wdc.shopping.business.impl.sgbd.repository.purchaseitem.PurchaseItemRepositoryImpl());
    }

    public static void release() {
        UserRepository.BEAN.set(null);
        ProductRepository.BEAN.set(null);
        PurchaseRepository.BEAN.set(null);
        PurchaseItemRepository.BEAN.set(null);
    }

}
