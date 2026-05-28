package br.com.wdc.shopping.domain.model;

public class PurchaseItem {

    public Long id;
    public Integer amount;
    public Double price;

    public Purchase purchase;
    public Product product;

    public Long purchaseId() {
        return purchase != null ? purchase.id : null;
    }

    public Long productId() {
        return product != null ? product.id : null;
    }

}
