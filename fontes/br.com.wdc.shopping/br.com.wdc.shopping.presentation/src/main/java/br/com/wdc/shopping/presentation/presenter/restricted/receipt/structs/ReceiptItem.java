package br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs;

import java.io.Serializable;
import java.util.Optional;

import br.com.wdc.shopping.business.shared.model.Product;
import br.com.wdc.shopping.business.shared.model.PurchaseItem;
import br.com.wdc.shopping.business.shared.utils.ProjectionValues;

public class ReceiptItem implements Serializable {

    private static final long serialVersionUID = -646852243709419945L;

    public long id;
    public String description;
    public double value;
    public int quantity;

    public static PurchaseItem projection() {
        var pv = ProjectionValues.INSTANCE;

        var prdPrj = new Product();
        prdPrj.name = pv.str;

        var prj = new PurchaseItem();
        prj.id = pv.i64;
        prj.price = pv.f64;
        prj.amount = pv.i32;
        prj.product = prdPrj;
        return prj;
    }

    public static ReceiptItem create(PurchaseItem src) {
        if (src == null) {
            return null;
        }

        var tgt = new ReceiptItem();

        tgt.value = Optional.ofNullable(src.price).orElse(0.0);
        tgt.quantity = Optional.ofNullable(src.amount).orElse(0);

        if (src.product != null) {
            tgt.description = src.product.name;
        }

        return tgt;
    }

}
