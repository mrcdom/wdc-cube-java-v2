package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
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

		var itemPrj = new PurchaseItem();
		itemPrj.id = pv.i64;
		itemPrj.amount = pv.i32;
		itemPrj.product = new Product();
		itemPrj.product.id = pv.i64;

		var itemCriteria = new PurchaseItemCriteria()
				.withProductId(DBReset.BOLA_WILSON_ID);

		var projection = new Purchase();
		projection.id = pv.i64;
		projection.items = pv.singletonList(itemPrj, itemCriteria);

		var purchase = repo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ID, projection);
		assertNotNull(purchase);
		assertNotNull(purchase.items);
		assertEquals(1, purchase.items.size());
		assertEquals(DBReset.BOLA_WILSON_ID, purchase.items.get(0).product.id);
	}
}
