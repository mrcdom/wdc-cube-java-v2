package br.com.wdc.shopping.presentation.presenter.restricted.cart.structs;

import java.io.Serializable;

import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;

public class CartItem implements Serializable {

    private static final long serialVersionUID = 643486220509070023L;

    public long id;
    public String image;
    public String name;
    public double price;
    public int quantity;

    public static CartItem create(ProductInfo product, int quantity) {
        var item = new CartItem();
        item.id = product.id;
        item.name = product.name;
        item.image = product.image;
        item.price = product.price;
        item.quantity = quantity;
        return item;
    }
}
