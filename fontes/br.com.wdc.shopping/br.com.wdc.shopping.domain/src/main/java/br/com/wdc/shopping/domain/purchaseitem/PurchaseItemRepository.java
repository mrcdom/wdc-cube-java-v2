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
                .withPurchase(new Purchase().withId(pv.i64))
                .withProduct(new Product().withId(pv.i64));
    }

    /**
     * Busca pela chave: um {@code PurchaseItemCriteria} com igualdade sobre a chave primária, resolvido pelo {@code fetch}.
     *
     * @param projection {@code null} projeta {@link #newProjection()} — todos os campos rasos da entidade.
     * @return a entidade, ou {@code null} se não houver linha com essa chave.
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
