package br.com.wdc.shopping.domain.repositories;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.utils.ProjectionValues;

public interface PurchaseRepository extends Repository<Purchase, PurchaseCriteria, Long> {

    AtomicReference<PurchaseRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Purchase newProjection() {
        var pv = ProjectionValues.INSTANCE;

        Purchase prj = new Purchase();
        prj.id = pv.i64;
        prj.buyDate = pv.offsetDateTime;
        prj.user = new User();
        prj.user.id = pv.i64;
        return prj;
    }


}
