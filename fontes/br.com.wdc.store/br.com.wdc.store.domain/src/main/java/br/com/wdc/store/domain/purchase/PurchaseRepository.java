package br.com.wdc.store.domain.purchase;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;
import br.com.wdc.store.domain.user.User;

public interface PurchaseRepository extends Repository<Purchase, PurchaseCriteria, Long> {

    AtomicReference<PurchaseRepository> BEAN = new AtomicReference<>();
    
    @Override
    default Purchase newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new Purchase()
                .withId(pv.i64)
                .withBuyDate(pv.offsetDateTime)
                .withUser(new User()
                        .withId(pv.i64));
    }

    /**
     * Lookup by key: a {@code PurchaseCriteria} with equality on the primary key, resolved by {@code fetch}.
     *
     * @param projection {@code null} projects {@link #newProjection()} — all the entity's shallow fields.
     * @return the entity, or {@code null} if there is no row with that key.
     */
    @Override
    default Purchase fetchById(Long purchaseId, Purchase projection) {
        if (purchaseId == null) {
            return null;
        }

        var found = fetch(new PurchaseCriteria()
                .withPurchaseId(purchaseId)
                .withProjection(projection != null ? projection : newProjection()), 0, 1);

        return found.isEmpty() ? null : found.get(0);
    }


}
