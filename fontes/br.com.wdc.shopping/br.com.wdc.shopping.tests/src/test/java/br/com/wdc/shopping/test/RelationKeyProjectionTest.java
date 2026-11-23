package br.com.wdc.shopping.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.persistence.impl.repository.PurchaseItemRepositoryImpl;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

/**
 * The foreign-key shortcut: an association projected <b>by its key only</b> comes from the row itself, without a
 * subselect.
 *
 * <p>
 * The standard projection of {@link PurchaseItem} brings {@code purchase} and {@code product} only to carry the id —
 * two subselects per row that would merely rediscover the foreign-key column already present. With the key declared
 * in {@code PurchaseItemRepositoryImpl}, those subselects are not emitted.
 * </p>
 *
 * <p>
 * The tests inspect the generated SQL, not just the result: the result would still be correct <b>with</b> the
 * subselects, which is exactly what is meant to be eliminated. The last two make sure the shortcut does not swallow
 * the legitimate case — a projection asking for any field beyond the key queries the other side.
 * </p>
 */
public class RelationKeyProjectionTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    /** SQL of the PurchaseItem SELECT for the given projection. */
    private static String sqlFor(PurchaseItem projection) {
        return PurchaseItemRepositoryImpl.QUERY
                .select(projection, (t, q) -> q.where(t.ID.eq(1L)))
                .getSQL();
    }

    /**
     * How many times the table appears in a {@code FROM}.
     *
     * <p>
     * jOOQ qualifies the table with the schema — {@code from "PUBLIC"."EN_PRODUCT" "p2"} —, so the bare name does not
     * work as an anchor.
     * </p>
     */
    private static int subselectsOn(String sql, String table) {
        var needle = "\"" + table + "\"";
        int count = 0;
        int i = sql.indexOf(needle);
        while (i >= 0) {
            count++;
            i = sql.indexOf(needle, i + 1);
        }
        return count;
    }

    @Test
    public void projectionWithOnlyKey_emitsNoSubselect() {
        String sql = sqlFor(env.purchaseItemRepo().newProjection());

        assertEquals("purchase projected by its key only must not generate a subselect\n" + sql,
                0, subselectsOn(sql, "EN_PURCHASE"));
        assertEquals("product projected by its key only must not generate a subselect\n" + sql,
                0, subselectsOn(sql, "EN_PRODUCT"));
    }

    @Test
    public void projectionWithOnlyKey_stillCarriesTheKeyValue() {
        PurchaseItem item = env.purchaseItemRepo()
                .fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, env.purchaseItemRepo().newProjection());

        assertNotNull(item);
        assertNotNull("the association is still returned, built from the row itself", item.purchase());
        assertNotNull(item.product());
        assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, item.purchase().id());
        assertEquals(DBReset.COFFEE_MAKER_ID, item.product().id());
        // Nothing beyond the key is fetched — exactly what the shortcut promises.
        assertNull(item.product().name());
    }

    @Test
    public void projectionBeyondKey_goesBackToSubselect() {
        PurchaseItem prj = env.purchaseItemRepo().newProjection();
        prj.product()
                .withName(ProjectionValues.INSTANCE.str);

        String sql = sqlFor(prj);

        assertTrue("asking for product.name requires querying the other side",
                subselectsOn(sql, "EN_PRODUCT") > 0);
        assertEquals("purchase still has only the key, and stays without a subselect\n" + sql,
                0, subselectsOn(sql, "EN_PURCHASE"));
    }

    @Test
    public void projectionBeyondKey_bringsTheRequestedField() {
        PurchaseItem prj = env.purchaseItemRepo().newProjection();
        prj.product()
                .withName(ProjectionValues.INSTANCE.str);

        PurchaseItem item = env.purchaseItemRepo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, prj);

        assertNotNull(item);
        assertNotNull(item.product());
        assertEquals(DBReset.COFFEE_MAKER_ID, item.product().id());
        assertNotNull("the field requested beyond the key arrives filled in", item.product().name());
    }

    @Test
    public void unrelatedProjection_isUnaffected() {
        // A projection that asks for no association is left exactly as it is.
        var prj = new PurchaseItem()
                .withId(ProjectionValues.INSTANCE.i64)
                .withAmount(ProjectionValues.INSTANCE.i32);

        String sql = sqlFor(prj);

        assertEquals(0, subselectsOn(sql, "EN_PURCHASE"));
        assertEquals(0, subselectsOn(sql, "EN_PRODUCT"));
    }

    /** Guard rail: the projection used above really is the one that touches only the key. */
    @Test
    public void projectionUnderTest_touchesOnlyTheKeyOfEachRelation() {
        PurchaseItem prj = env.purchaseItemRepo().newProjection();

        Purchase purchase = prj.purchase();
        Product product = prj.product();

        assertNotNull(purchase.id());
        assertNull(purchase.buyDate());
        assertNull(purchase.user());
        assertNotNull(product.id());
        assertNull(product.name());
        assertNull(product.price());
    }
}
