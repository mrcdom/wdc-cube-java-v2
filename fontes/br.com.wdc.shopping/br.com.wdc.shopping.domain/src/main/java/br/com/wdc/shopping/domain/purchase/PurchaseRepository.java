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

    /**
     * Busca pela chave: um {@code PurchaseCriteria} com igualdade sobre a chave primária, resolvido pelo {@code fetch}.
     *
     * @param projection {@code null} projeta {@link #newProjection()} — todos os campos rasos da entidade.
     * @return a entidade, ou {@code null} se não houver linha com essa chave.
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
