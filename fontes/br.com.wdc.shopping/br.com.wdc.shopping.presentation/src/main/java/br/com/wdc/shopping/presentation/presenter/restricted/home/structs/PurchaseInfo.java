package br.com.wdc.shopping.presentation.presenter.restricted.home.structs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public class PurchaseInfo implements Serializable {

    private static final long serialVersionUID = 5212741192677222435L;

    public long id;
    public long date;
    public double total;
    public List<String> items;

    public static Purchase projectionWithItens() {
        var pv = ProjectionValues.INSTANCE;

        var prdPrj = new Product();
        prdPrj.name = pv.str;

        var itemPrj = new PurchaseItem();
        itemPrj.price = pv.f64;
        itemPrj.product = prdPrj;

        var prj = new Purchase();
        prj.id = pv.i64;
        prj.buyDate = pv.offsetDateTime;
        prj.items = Collections.singletonList(itemPrj);

        return prj;
    }

    public static PurchaseInfo create(Purchase src) {
        if (src == null) {
            return null;
        }

        var pv = ProjectionValues.INSTANCE;

        var tgt = new PurchaseInfo();
        tgt.id = Optional.ofNullable(src.id).orElse(-1L);

        var buyDate = CoerceUtils.asDate(src.buyDate);
        tgt.date = Optional.ofNullable(buyDate).orElse(pv.date).getTime();
        tgt.items = new ArrayList<>();

        var total = 0.0;
        for (var item : Optional.ofNullable(src.items).orElse(Collections.emptyList())) {
            total += Optional.ofNullable(item.price).orElse(0.0);

            if (item.product != null && StringUtils.isNotBlank(item.product.name)) {
                tgt.items.add(item.product.name);
            }
        }

        tgt.total = total;

        return tgt;
    }

}
