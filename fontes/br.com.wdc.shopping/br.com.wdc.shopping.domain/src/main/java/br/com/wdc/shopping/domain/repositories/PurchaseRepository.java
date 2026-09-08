package br.com.wdc.shopping.domain.repositories;

import br.com.wdc.framework.domain.repository.Repository;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.framework.domain.projection.ProjectionValues;

public interface PurchaseRepository extends Repository<Purchase, PurchaseCriteria, Long> {

    AtomicReference<PurchaseRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Purchase newProjection() {
        var pv = ProjectionValues.INSTANCE;

        Purchase prj = new Purchase()
                .withId(pv.i64)
                .withBuyDate(pv.offsetDateTime)
                .withUser(new User().withId(pv.i64));
        return prj;
    }


}
