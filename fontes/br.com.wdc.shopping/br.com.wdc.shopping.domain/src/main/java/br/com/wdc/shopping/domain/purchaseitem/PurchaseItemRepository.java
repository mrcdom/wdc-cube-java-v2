package br.com.wdc.shopping.domain.purchaseitem;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.repository.Repository;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;

public interface PurchaseItemRepository extends Repository<PurchaseItem, PurchaseItemCriteria, Long> {

    AtomicReference<PurchaseItemRepository> BEAN = new AtomicReference<>();

    @Override
    default PurchaseItem newProjection() {
        var pv = ProjectionValues.INSTANCE;

        return new PurchaseItem()
                .withId(pv.i64)
                .withAmount(pv.i32)
                .withPrice(pv.f64)
                .withPurchase(new Purchase()
                        .withId(pv.i64))
                .withProduct(new Product()
                        .withId(pv.i64));
    }

    /**
     * Lookup by key: a {@code PurchaseItemCriteria} with equality on the primary key, resolved by {@code fetch}.
     *
     * @param projection {@code null} projects {@link #newProjection()} — all the entity's shallow fields.
     * @return the entity, or {@code null} if there is no row with that key.
     */
    @Override
    default PurchaseItem fetchById(Long purchaseItemId, PurchaseItem projection) {
        if (purchaseItemId == null) {
            return null;
        }

        var found = fetch(new PurchaseItemCriteria()
                .withPurchaseItemId(purchaseItemId)
                .withProjection(projection != null ? projection : newProjection()), 0, 1);

        return found.isEmpty() ? null : found.get(0);
    }

}
