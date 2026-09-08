package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

public class PurchaseRepositoryTest extends AbstractPurchaseRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected PurchaseRepository repo() {
		return env.purchaseRepo();
	}

	@Override
	protected PurchaseItemRepository purchaseItemRepo() {
		return env.purchaseItemRepo();
	}

	// -- Teste exclusivo do modo LOCAL (ProjectionList com sub-criteria) --

	@Test
	public void fetchWithProjectionList_filterItemsByCriteria() {
		var pv = ProjectionValues.INSTANCE;

		var itemPrj = new PurchaseItem()
				.withId(pv.i64)
				.withAmount(pv.i32)
				.withProduct(new Product().withId(pv.i64));

		var itemCriteria = new PurchaseItemCriteria()
				.withProductId(DBReset.BOLA_WILSON_ID);

		var projection = new Purchase()
				.withId(pv.i64)
				.withItems(pv.singletonList(itemPrj, itemCriteria));

		var purchase = repo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ID, projection);
		assertNotNull(purchase);
		assertNotNull(purchase.items());
		assertEquals(1, purchase.items().size());
		assertEquals(DBReset.BOLA_WILSON_ID, purchase.items().get(0).product().id());
	}
}
