package br.com.wdc.store.test.repository;

import static br.com.wdc.store.persistence.impl.scheme.Tables.EN_PURCHASE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.projection.ProjectionSet;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.jooq.JsonQuery;
import br.com.wdc.framework.jooq.JsonQueryBuilder;
import br.com.wdc.store.domain.product.Product;
import br.com.wdc.store.domain.purchaseitem.PurchaseItem;
import br.com.wdc.store.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.store.persistence.impl.StoreDSLContext;
import br.com.wdc.store.persistence.impl.repository.PurchaseItemRepositoryImpl;
import br.com.wdc.store.persistence.impl.scheme.tables.EnPurchase;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * A 1:N relation held in a {@code Set} ({@code JsonQueryBuilder.addBeanSetField}), projected with a
 * {@link ProjectionSet}: the criteria it carries filter and order the children, and its slice cuts them, as for a
 * {@code List}. The order survives because the set read back is a {@code LinkedHashSet}.
 *
 * <p>
 * No entity of the store keeps a relation in a {@code Set}, so the test declares its own: a purchase whose items are a
 * set, over the store's tables. The wire codec carries projected collections as lists, so this is the server-side path.
 * </p>
 */
public class ProjectionSetTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    /** A purchase with its items in a set. */
    public static final class SetPurchase {

        private Long id;
        private Set<PurchaseItem> items;

        public Long id() {
            return id;
        }

        public SetPurchase withId(Long id) {
            this.id = id;
            return this;
        }

        public Set<PurchaseItem> items() {
            return items;
        }

        public SetPurchase withItems(Set<PurchaseItem> items) {
            this.items = items;
            return this;
        }
    }

    // @formatter:off
    private static final JsonQuery<SetPurchase, EnPurchase> QUERY = new JsonQueryBuilder<SetPurchase, EnPurchase>()
            .setAlias("sp")
            .setBeanFactory(SetPurchase::new)
            .setTableFactory(EN_PURCHASE::as)
            .setDSLContextSupplier(() -> StoreDSLContext.BEAN.get())
            .addI64("id", p -> p.id(), (p, v) -> p.withId(v), t -> t.ID)
            .addBeanSetField("items", p -> p.items(), (p, v) -> p.withItems(v), PurchaseItemRepositoryImpl.QUERY, cq -> {
                var enPurchase = cq.getSuperTable();
                var enPurchaseItem = cq.getChildTable();

                cq.dsl().where()
                    .and(enPurchaseItem.PURCHASEID.eq(enPurchase.ID))
                    .and(PurchaseItemRepositoryImpl.applyConditions(cq));
            })
            .build();
    // @formatter:on

    private static PurchaseItem itemProjection() {
        var pv = ProjectionValues.INSTANCE;
        return new PurchaseItem()
                .withId(pv.i64)
                .withAmount(pv.i32)
                .withProduct(new Product()
                        .withId(pv.i64));
    }

    private static ProjectionSet<PurchaseItem> items(PurchaseItemCriteria criteria) {
        return ProjectionValues.INSTANCE.singletonSet(itemProjection(), criteria);
    }

    private static ProjectionSet<PurchaseItem> items(PurchaseItemCriteria.OrderBy order) {
        return items(new PurchaseItemCriteria()
                .withOrderBy(order));
    }

    private static List<Long> idsOf(ProjectionSet<PurchaseItem> items) {
        var pv = ProjectionValues.INSTANCE;
        var found = QUERY.fetchToList(new SetPurchase()
                .withId(pv.i64)
                .withItems(items), (t, q) -> q.where(t.ID.eq(DBReset.ADMIN_SECOND_PURCHASE_ID)));
        assertEquals(1, found.size());
        var purchase = found.get(0);
        assertNotNull(purchase.items());
        assertTrue("the set read back keeps the order", purchase.items() instanceof LinkedHashSet);
        return purchase.items()
                .stream()
                .map(PurchaseItem::id)
                .toList();
    }

    @Test
    public void withoutOrderOrSlice_bringsEveryItem() {
        assertEquals(2, idsOf(items(new PurchaseItemCriteria())).size());
    }

    @Test
    public void ascending_orders() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
                idsOf(items(PurchaseItemCriteria.OrderBy.OLDEST_FIRST)));
    }

    @Test
    public void descending_reverses() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID),
                idsOf(items(PurchaseItemCriteria.OrderBy.NEWEST_FIRST)));
    }

    @Test
    public void limit_respectsOrder() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
                idsOf(items(PurchaseItemCriteria.OrderBy.NEWEST_FIRST)
                        .withLimit(1)));
    }

    @Test
    public void offset_skips() {
        assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
                idsOf(items(PurchaseItemCriteria.OrderBy.OLDEST_FIRST)
                        .withOffset(1)));
    }

    @Test
    public void subCriteria_filters() {
        var ids = idsOf(items(new PurchaseItemCriteria()
                .withProductId(DBReset.WILSON_BALL_ID)));
        assertEquals(1, ids.size());
    }
}
