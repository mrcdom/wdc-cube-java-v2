package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class PurchaseItem implements KeyedEntity {

    public Long id;
    public Integer amount;
    public Double price;

    public Purchase purchase;
    public Product product;

    @Override
    public Long key() {
        return id;
    }

    public Long purchaseId() {
        return purchase != null ? purchase.id : null;
    }

    public Long productId() {
        return product != null ? product.id : null;
    }

}
