package br.com.wdc.shopping.domain.purchase;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;
import br.com.wdc.shopping.domain.user.User;

public interface PurchaseRepository extends Repository<Purchase, PurchaseCriteria, Long> {

    AtomicReference<PurchaseRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Purchase newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new Purchase()
                .withId(pv.i64)
                .withBuyDate(pv.offsetDateTime)
                .withUser(new User().withId(pv.i64));
    }


}
