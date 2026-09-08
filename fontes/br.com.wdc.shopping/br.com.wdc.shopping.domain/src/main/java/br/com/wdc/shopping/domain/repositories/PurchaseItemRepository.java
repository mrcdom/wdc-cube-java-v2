package br.com.wdc.shopping.domain.repositories;

import br.com.wdc.framework.domain.repository.Repository;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public interface PurchaseItemRepository extends Repository<PurchaseItem, PurchaseItemCriteria, Long> {

    AtomicReference<PurchaseItemRepository> BEAN = new AtomicReference<>();

    @Override
    default PurchaseItem newProjection() {
        var pv = ProjectionValues.INSTANCE;

        PurchaseItem prj = new PurchaseItem()
                .withId(pv.i64)
                .withAmount(pv.i32)
                .withPrice(pv.f64)
                .withPurchase(new Purchase().withId(pv.i64))
                .withProduct(new Product().withId(pv.i64));
        return prj;
    }

}
