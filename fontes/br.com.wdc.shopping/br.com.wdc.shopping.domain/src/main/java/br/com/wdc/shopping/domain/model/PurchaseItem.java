package br.com.wdc.shopping.domain.model;

import br.com.wdc.framework.commons.serialization.KeyedEntity;

public class PurchaseItem implements KeyedEntity {

    private Long id;

    public Long id() {
        return id;
    }

    public PurchaseItem withId(Long id) {
        this.id = id;
        return this;
    }

    private Integer amount;

    public Integer amount() {
        return amount;
    }

    public PurchaseItem withAmount(Integer amount) {
        this.amount = amount;
        return this;
    }

    private Double price;

    public Double price() {
        return price;
    }

    public PurchaseItem withPrice(Double price) {
        this.price = price;
        return this;
    }

    private Purchase purchase;

    public Purchase purchase() {
        return purchase;
    }

    public PurchaseItem withPurchase(Purchase purchase) {
        this.purchase = purchase;
        return this;
    }

    private Product product;

    public Product product() {
        return product;
    }

    public PurchaseItem withProduct(Product product) {
        this.product = product;
        return this;
    }

    @Override
    public Long key() {
        return id;
    }

    public Long purchaseId() {
        return purchase != null ? purchase.id() : null;
    }

    public Long productId() {
        return product != null ? product.id() : null;
    }

}
