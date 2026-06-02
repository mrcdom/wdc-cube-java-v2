package br.com.wdc.shopping.domain.repositories;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public interface PurchaseItemRepository extends Repository<PurchaseItem, PurchaseItemCriteria, Long> {

    AtomicReference<PurchaseItemRepository> BEAN = new AtomicReference<>();

    @Override
    default PurchaseItem newProjection() {
        var pv = ProjectionValues.INSTANCE;

        PurchaseItem prj = new PurchaseItem();
        prj.id = pv.i64;
        prj.amount = pv.i32;
        prj.price = pv.f64;
        prj.purchase = new Purchase();
        prj.purchase.id = pv.i64;
        prj.product = new Product();
        prj.product.id = pv.i64;
        return prj;
    }

}
