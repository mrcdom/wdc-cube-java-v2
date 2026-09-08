package br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs;

import java.io.Serializable;
import java.util.Optional;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;

public class ReceiptItem implements Serializable {

    private static final long serialVersionUID = -646852243709419945L;

    public long id;
    public String description;
    public double value;
    public int quantity;

    public static PurchaseItem projection() {
        var pv = ProjectionValues.INSTANCE;

        var prdPrj = new Product()
                .withName(pv.str);

        var prj = new PurchaseItem()
                .withId(pv.i64)
                .withPrice(pv.f64)
                .withAmount(pv.i32)
                .withProduct(prdPrj);
        return prj;
    }

    public static ReceiptItem create(PurchaseItem src) {
        if (src == null) {
            return null;
        }

        var tgt = new ReceiptItem();

        tgt.value = Optional.ofNullable(src.price()).orElse(0.0);
        tgt.quantity = Optional.ofNullable(src.amount()).orElse(0);

        if (src.product() != null) {
            tgt.description = src.product().name();
        }

        return tgt;
    }

}
