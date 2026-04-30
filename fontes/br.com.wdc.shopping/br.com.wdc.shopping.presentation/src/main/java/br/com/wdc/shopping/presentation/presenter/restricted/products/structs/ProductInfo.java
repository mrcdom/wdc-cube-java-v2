package br.com.wdc.shopping.presentation.presenter.restricted.products.structs;

import java.io.Serializable;
import java.util.Optional;

import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class ProductInfo implements Serializable {

    private static final long serialVersionUID = 4135464659947783296L;

    public long id;
    public String image;
    public String name;
    public String description;
    public double price;
    
    public static Product projection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new Product();
        prj.id = pv.i64;
        prj.name = pv.str;
        prj.price = pv.f64;
        prj.description = pv.str;
        return prj;
    }

    public static ProductInfo create(Product product) {
        if (product == null) {
            return null;
        }

        var item = new ProductInfo();
        item.id = Optional.ofNullable(product.id).orElse(-1L);
        item.name = Optional.ofNullable(product.name).orElse("unknown");
        item.price = Optional.ofNullable(product.price).orElse(0.0);
        item.description = Optional.ofNullable(product.description).orElse("unknown");
        item.image = "image/product/" + item.id + ".png";
        return item;
    }
}
