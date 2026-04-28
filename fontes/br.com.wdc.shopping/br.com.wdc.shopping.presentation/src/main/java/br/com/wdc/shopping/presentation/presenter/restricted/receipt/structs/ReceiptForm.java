package br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.wdc.shopping.business.shared.model.Purchase;
import br.com.wdc.shopping.business.shared.utils.ProjectionValues;

public class ReceiptForm implements Serializable {

    private static final long serialVersionUID = -3783933804174247490L;

    public Long date;
    public List<ReceiptItem> items;
    public Double total;

    public static Purchase projection() {
        var pv = ProjectionValues.INSTANCE;

        var prj = new Purchase();
        prj.buyDate = pv.offsetDateTime;
        prj.items = Collections.singletonList(ReceiptItem.projection());

        return prj;
    }

    public static ReceiptForm create(Purchase src) {
        if (src == null) {
            return null;
        }

        var tgt = new ReceiptForm();

        tgt.date = src.buyDate == null ? null : src.buyDate.toInstant().toEpochMilli();
        tgt.items = new ArrayList<>();

        var total = 0.0;
        if (src.items != null) {
            for (var purchaseItem : src.items) {
                if (purchaseItem.price != null) {
                    total += purchaseItem.price;
                }

                tgt.items.add(ReceiptItem.create(purchaseItem));
            }
        }
        tgt.total = total;

        return tgt;
    }

}
