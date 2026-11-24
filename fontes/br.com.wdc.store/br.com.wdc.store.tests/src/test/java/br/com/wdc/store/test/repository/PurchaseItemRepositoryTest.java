package br.com.wdc.store.test.repository;

import static org.junit.Assert.*;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.store.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

public class PurchaseItemRepositoryTest extends AbstractPurchaseItemRepositoryTest {

	@ClassRule
	public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

	@Rule
	public ExternalResource resetDb = new ResetDatabaseRule(env);

	@Override
	protected PurchaseItemRepository repo() {
		return env.purchaseItemRepo();
	}

	// -- LOCAL-mode-only tests (they assert on purchase, which REST does not return) --

	@Test
	public void fetchById_returnsCorrectItem_withPurchase() {
		var item = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, projectionWithRelations());
		assertNotNull(item);
		assertNotNull(item.purchase());
	}

	@Test
	public void insert_newPurchaseItem_withPurchaseAssertion() {
		var item = new br.com.wdc.store.domain.purchaseitem.PurchaseItem()
				.withAmount(5)
				.withPrice(15.50)
				.withPurchase(new br.com.wdc.store.domain.purchase.Purchase()
						.withId(DBReset.ADMIN_FIRST_PURCHASE_ID))
				.withProduct(new br.com.wdc.store.domain.product.Product()
						.withId(DBReset.FLASH_DRIVE_2GB_ID));

		boolean inserted = repo().insert(item);
		assertTrue(inserted);

		var fetched = repo().fetchById(item.id(), projectionWithRelations());
		assertNotNull(fetched);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, fetched.purchase().id());
		assertEquals(DBReset.FLASH_DRIVE_2GB_ID, fetched.product().id());
	}

	@Test
	public void insert_withoutPrice_recordsTheProductPrice_andReturnsIt() {
		var item = new br.com.wdc.store.domain.purchaseitem.PurchaseItem()
				.withAmount(1)
				.withPurchase(new br.com.wdc.store.domain.purchase.Purchase()
						.withId(DBReset.ADMIN_FIRST_PURCHASE_ID))
				.withProduct(new br.com.wdc.store.domain.product.Product()
						.withId(DBReset.FLASH_DRIVE_2GB_ID));

		assertTrue(repo().insert(item));

		assertEquals("RETURNING puts the recorded price on the item", 16.0, item.price(), 0.001);
		var fetched = repo().fetchById(item.id(), new br.com.wdc.store.domain.purchaseitem.PurchaseItem()
				.withPrice(br.com.wdc.framework.domain.projection.ProjectionValues.INSTANCE.f64));
		assertEquals(16.0, fetched.price(), 0.001);
	}

	@Test
	public void insert_withPrice_keepsIt() {
		var item = new br.com.wdc.store.domain.purchaseitem.PurchaseItem()
				.withAmount(1)
				.withPrice(12.5)
				.withPurchase(new br.com.wdc.store.domain.purchase.Purchase()
						.withId(DBReset.ADMIN_FIRST_PURCHASE_ID))
				.withProduct(new br.com.wdc.store.domain.product.Product()
						.withId(DBReset.FLASH_DRIVE_2GB_ID));

		assertTrue(repo().insert(item));

		assertEquals(12.5, item.price(), 0.001);
	}
}
