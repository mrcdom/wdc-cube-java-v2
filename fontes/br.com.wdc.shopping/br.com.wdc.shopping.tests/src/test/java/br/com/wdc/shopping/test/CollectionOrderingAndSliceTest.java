package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.projection.ProjectionList;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

/**
 * Ordem e recorte da coleção filha, pedidos na própria projeção.
 *
 * <p>
 * A coleção sai de uma subconsulta correlacionada com a linha do pai, e é isso que dita a forma do SQL: a ordem entra
 * <b>dentro</b> da agregação, e o recorte sai como um {@code IN} sobre a chave primária — uma subconsulta que, por ser
 * correlacionada, enxerga o pai. Envolver a coleção numa tabela derivada, onde caberia um {@code ORDER BY ... LIMIT}
 * comum, poria a correlação fora de alcance.
 * </p>
 *
 * <p>
 * A segunda compra do admin tem dois itens, de ids 1 e 2 — o suficiente para que ordem e corte se distingam de uma
 * coleção devolvida inteira e na ordem do banco.
 * </p>
 */
public class CollectionOrderingAndSliceTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    /** Projeção de item: id e o produto só pela chave. */
    private static PurchaseItem itemProjection() {
        var pv = ProjectionValues.INSTANCE;
        return new PurchaseItem()
                .withId(pv.i64)
                .withAmount(pv.i32)
                .withProduct(new Product().withId(pv.i64));
    }

    /** Busca a segunda compra do admin com a coleção de itens montada pelo chamador. */
    private static List<PurchaseItem> itemsOf(ProjectionList<PurchaseItem> items) {
        var pv = ProjectionValues.INSTANCE;
        var purchase = env.purchaseRepo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ID,
                new Purchase().withId(pv.i64).withItems(items));

        assertNotNull(purchase);
        assertNotNull(purchase.items());
        return purchase.items();
    }

    private static List<Long> idsOf(List<PurchaseItem> items) {
        return items.stream().map(PurchaseItem::id).toList();
    }

    private static ProjectionList<PurchaseItem> items(PurchaseItemCriteria.OrderBy order) {
        var criteria = new PurchaseItemCriteria().withOrderBy(order);
        return ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria);
    }

    @Test
    public void withoutOrderOrSlice_bringsEveryItem() {
        var criteria = new PurchaseItemCriteria();
        var items = itemsOf(ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria));

        assertEquals(2, items.size());
    }

    @Test
    public void ascending_ordersTheCollection() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
                idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING))));
    }

    @Test
    public void descending_reversesTheCollection() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID),
                idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.DESCENDING))));
    }

    @Test
    public void limit_cutsTheCollection() {
        var items = itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withLimit(1));

        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID), idsOf(items));
    }

    @Test
    public void limit_respectsTheOrderItWasGiven() {
        // Mesmo limite, ordem invertida: o item que sobra tem de ser o outro — é o que separa "cortou depois de
        // ordenar" de "cortou e por acaso ordenou".
        var items = itemsOf(items(PurchaseItemCriteria.OrderBy.DESCENDING).withLimit(1));

        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID), idsOf(items));
    }

    @Test
    public void offset_skipsFromTheStart() {
        var items = itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(1));

        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID), idsOf(items));
    }

    @Test
    public void limitAndOffset_together() {
        var items = itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(1).withLimit(1));

        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID), idsOf(items));
    }

    @Test
    public void offsetBeyondTheEnd_bringsNothing() {
        var items = itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(10));

        assertEquals(List.of(), idsOf(items));
    }

    @Test
    public void sliceComposesWithTheCollectionFilter() {
        // O recorte repete o mesmo filtro da coleção — sem isso, o IN escolheria entre linhas que o filtro já
        // descartaria, e o corte cairia sobre o conjunto errado.
        var criteria = new PurchaseItemCriteria()
                .withProductId(DBReset.BOLA_WILSON_ID)
                .withOrderBy(PurchaseItemCriteria.OrderBy.ASCENDING);
        var items = itemsOf(ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria).withLimit(5));

        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID), idsOf(items));
        assertEquals(DBReset.BOLA_WILSON_ID, items.get(0).product().id());
    }
}
