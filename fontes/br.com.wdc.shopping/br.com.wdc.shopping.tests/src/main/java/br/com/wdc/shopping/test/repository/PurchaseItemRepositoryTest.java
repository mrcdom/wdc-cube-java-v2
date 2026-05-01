package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.ResetDatabaseRule;
import br.com.wdc.shopping.test.util.TestEnvironment;

public class PurchaseItemRepositoryTest extends AbstractPurchaseItemRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected PurchaseItemRepository repo() {
		return env.purchaseItemRepo();
	}

	// -- Testes exclusivos do modo LOCAL (assertam purchase, que REST não retorna) --

	@Test
	public void fetchById_returnsCorrectItem_withPurchase() {
		var item = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, projectionWithRelations());
		assertNotNull(item);
		assertNotNull(item.purchase);
	}

	@Test
	public void insert_newPurchaseItem_withPurchaseAssertion() {
		var item = new br.com.wdc.shopping.domain.model.PurchaseItem();
		item.amount = 5;
		item.price = 15.50;
		item.purchase = new br.com.wdc.shopping.domain.model.Purchase();
		item.purchase.id = DBReset.ADMIN_FIRST_PURCHASE_ID;
		item.product = new br.com.wdc.shopping.domain.model.Product();
		item.product.id = DBReset.PEN_DRIVE2GB_ID;

		boolean inserted = repo().insert(item);
		assertTrue(inserted);

		var fetched = repo().fetchById(item.id, projectionWithRelations());
		assertNotNull(fetched);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, fetched.purchase.id);
		assertEquals(DBReset.PEN_DRIVE2GB_ID, fetched.product.id);
	}
}
