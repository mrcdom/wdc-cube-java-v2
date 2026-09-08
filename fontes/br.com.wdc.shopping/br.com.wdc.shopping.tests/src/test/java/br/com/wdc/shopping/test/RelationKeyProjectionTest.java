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
 * O atalho de chave estrangeira: uma associação projetada <b>só pela chave</b> sai da própria linha, sem subselect.
 *
 * <p>
 * A projeção corrente de {@link PurchaseItem} traz {@code purchase} e {@code product} apenas para carregar o id —
 * dois subselects por linha que só redescobriam a coluna da chave estrangeira que já está ali. Declarada a chave em
 * {@code PurchaseItemRepositoryImpl}, esses subselects deixam de ser emitidos.
 * </p>
 *
 * <p>
 * Os testes olham o SQL gerado, e não só o resultado: o resultado continuaria correto <b>com</b> os subselects, que é
 * justamente o que se quer eliminar. Os dois últimos garantem que o atalho não come o caso legítimo — projeção que
 * pede qualquer campo além da chave volta a consultar o outro lado.
 * </p>
 */
public class RelationKeyProjectionTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    /** SQL do SELECT de PurchaseItem para a projeção informada. */
    private static String sqlFor(PurchaseItem projection) {
        return PurchaseItemRepositoryImpl.QUERY
                .select(projection, (t, q) -> q.where(t.ID.eq(1L)))
                .getSQL();
    }

    /**
     * Quantas vezes a tabela aparece num {@code FROM}.
     *
     * <p>
     * O jOOQ qualifica a tabela com o esquema — {@code from "PUBLIC"."EN_PRODUCT" "p2"} —, então o nome sozinho não
     * serve de âncora.
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

        assertEquals("purchase projetado só pela chave não deve gerar subselect",
                0, subselectsOn(sql, "EN_PURCHASE"));
        assertEquals("product projetado só pela chave não deve gerar subselect",
                0, subselectsOn(sql, "EN_PRODUCT"));
    }

    @Test
    public void projectionWithOnlyKey_stillCarriesTheKeyValue() {
        PurchaseItem item = env.purchaseItemRepo()
                .fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, env.purchaseItemRepo().newProjection());

        assertNotNull(item);
        assertNotNull("a associação continua vindo, montada da própria linha", item.purchase());
        assertNotNull(item.product());
        assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, item.purchase().id());
        assertEquals(DBReset.CAFETEIRA_ID, item.product().id());
        // Nada além da chave é trazido — é exatamente o que o atalho promete.
        assertNull(item.product().name());
    }

    @Test
    public void projectionBeyondKey_goesBackToSubselect() {
        PurchaseItem prj = env.purchaseItemRepo().newProjection();
        prj.product().withName(ProjectionValues.INSTANCE.str);

        String sql = sqlFor(prj);

        assertTrue("pedir product.name volta a exigir a consulta ao outro lado",
                subselectsOn(sql, "EN_PRODUCT") > 0);
        assertEquals("purchase continua só com a chave, e segue sem subselect",
                0, subselectsOn(sql, "EN_PURCHASE"));
    }

    @Test
    public void projectionBeyondKey_bringsTheRequestedField() {
        PurchaseItem prj = env.purchaseItemRepo().newProjection();
        prj.product().withName(ProjectionValues.INSTANCE.str);

        PurchaseItem item = env.purchaseItemRepo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, prj);

        assertNotNull(item);
        assertNotNull(item.product());
        assertEquals(DBReset.CAFETEIRA_ID, item.product().id());
        assertNotNull("o campo pedido além da chave chega preenchido", item.product().name());
    }

    @Test
    public void unrelatedProjection_isUnaffected() {
        // Projeção que não pede associação alguma continua exatamente como era.
        var prj = new PurchaseItem()
                .withId(ProjectionValues.INSTANCE.i64)
                .withAmount(ProjectionValues.INSTANCE.i32);

        String sql = sqlFor(prj);

        assertEquals(0, subselectsOn(sql, "EN_PURCHASE"));
        assertEquals(0, subselectsOn(sql, "EN_PRODUCT"));
    }

    /** Guarda-corpo: a projeção usada acima é mesmo a que só toca a chave. */
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
