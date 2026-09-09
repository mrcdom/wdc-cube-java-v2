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
     * Busca pela chave — um {@code purchaseId} com filtro de igualdade sobre a chave primária.
     *
     * <p>
     * Mora aqui, e não em {@code Repository}, porque o contrato genérico não tem como montar o critério: ele conhece
     * o tipo {@code C}, mas não qual dos campos dele é a chave. Cada entidade sabe, e é só o que falta — o resto é o
     * {@code fetch} que todas as implementações já têm. Assim a busca por chave é a mesma consulta das outras, com o
     * mesmo tratamento de projeção, de segurança e de transação, em vez de um caminho paralelo por implementação.
     * </p>
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
