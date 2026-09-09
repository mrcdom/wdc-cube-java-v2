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
     * Busca pela chave — um {@code purchaseItemId} com filtro de igualdade sobre a chave primária.
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
