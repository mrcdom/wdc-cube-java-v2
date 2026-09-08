package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.scripts.sgbd.DBReset;

public abstract class AbstractPurchaseItemRepositoryTest {

	protected abstract PurchaseItemRepository repo();

	protected PurchaseItem projectionWithRelations() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new PurchaseItem()
				.withId(pv.i64)
				.withAmount(pv.i32)
				.withPrice(pv.f64)
				.withPurchase(new Purchase().withId(pv.i64))
				.withProduct(new Product().withId(pv.i64));
		return prj;
	}

	// :: fetch

	@Test
	public void fetchAll_returnsAllSeededItems() {
		List<PurchaseItem> items = repo().fetch(new PurchaseItemCriteria());
		assertEquals(3, items.size());
	}

	@Test
	public void fetchById_returnsCorrectItem() {
		var item = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, projectionWithRelations());
		assertNotNull(item);
		assertNotNull(item.amount());
		assertNotNull(item.price());
		assertNotNull(item.product());
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var item = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(item);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new PurchaseItem()
				.withId(pv.i64)
				.withAmount(pv.i32)
				.withPrice(pv.f64);

		var item = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, projection);
		assertNotNull(item);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, item.id());
		assertNotNull(item.amount());
		assertNotNull(item.price());
	}

	@Test
	public void fetchByPurchaseId_firstPurchase() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));
		assertEquals(1, items.size());
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, items.get(0).id());
	}

	@Test
	public void fetchByPurchaseId_secondPurchase() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_SECOND_PURCHASE_ID));
		assertEquals(2, items.size());
	}

	@Test
	public void fetchByUserId() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withUserId(DBReset.ADMIN_ID));
		assertEquals(3, items.size());
	}

	@Test
	public void fetchByUserId_noResults() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withUserId(DBReset.FULANO_ID));
		assertTrue(items.isEmpty());
	}

	@Test
	public void fetchByProductId() {
		var criteria = new PurchaseItemCriteria()
				.withProductId(DBReset.CAFETEIRA_ID)
				.withProjection(projectionWithRelations());
		var items = repo().fetch(criteria);
		assertFalse(items.isEmpty());
		for (var item : items) {
			assertEquals(DBReset.CAFETEIRA_ID, item.product().id());
		}
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.ASCENDING), 0, 2);
		assertEquals(2, items.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.ASCENDING));
		assertEquals(3, items.size());
		for (int i = 1; i < items.size(); i++) {
			assertTrue(items.get(i - 1).id() <= items.get(i).id());
		}
	}

	@Test
	public void fetchWithOrderDescending() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.DESCENDING));
		assertEquals(3, items.size());
		for (int i = 1; i < items.size(); i++) {
			assertTrue(items.get(i - 1).id() >= items.get(i).id());
		}
	}

	// :: count

	@Test
	public void countAll_returnsThree() {
		int count = repo().count(new PurchaseItemCriteria());
		assertEquals(3, count);
	}

	@Test
	public void countByPurchaseId() {
		int count = repo().count(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_SECOND_PURCHASE_ID));
		assertEquals(2, count);
	}

	@Test
	public void countByUserId() {
		int count = repo().count(new PurchaseItemCriteria()
				.withUserId(DBReset.ADMIN_ID));
		assertEquals(3, count);
	}

	@Test
	public void countNonExistent_returnsZero() {
		int count = repo().count(new PurchaseItemCriteria()
				.withPurchaseItemId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: insert

	@Test
	public void insert_newPurchaseItem() {
		var item = new PurchaseItem()
				.withAmount(5)
				.withPrice(15.50)
				.withPurchase(new Purchase().withId(DBReset.ADMIN_FIRST_PURCHASE_ID))
				.withProduct(new Product().withId(DBReset.PEN_DRIVE2GB_ID));

		boolean inserted = repo().insert(item);
		assertTrue(inserted);
		assertNotNull(item.id());

		var fetched = repo().fetchById(item.id(), projectionWithRelations());
		assertNotNull(fetched);
		assertEquals(Integer.valueOf(5), fetched.amount());
		assertEquals(15.50, fetched.price(), 0.001);
		assertEquals(DBReset.PEN_DRIVE2GB_ID, fetched.product().id());
	}

	// :: update

	@Test
	public void update_existingPurchaseItem() {
		var original = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, null);
		assertNotNull(original);

		var updated = new PurchaseItem()
				.withId(original.id())
				.withAmount(99)
				.withPrice(999.99)
				.withPurchase(original.purchase())
				.withProduct(original.product());

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, null);
		assertEquals(Integer.valueOf(99), fetched.amount());
		assertEquals(999.99, fetched.price(), 0.001);
	}

	// :: delete

	@Test
	public void deleteByPurchaseItemId() {
		int deleted = repo().delete(new PurchaseItemCriteria()
				.withPurchaseItemId(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID));
		assertEquals(1, deleted);
		assertEquals(2, repo().count(new PurchaseItemCriteria()));
	}

	@Test
	public void deleteByPurchaseId() {
		int deleted = repo().delete(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_SECOND_PURCHASE_ID));
		assertEquals(2, deleted);
		assertEquals(1, repo().count(new PurchaseItemCriteria()));
	}

	@Test
	public void deleteByUserId_crossEntityExists() {
		int deleted = repo().delete(new PurchaseItemCriteria()
				.withUserId(DBReset.ADMIN_ID));
		assertEquals(3, deleted);
		assertEquals(0, repo().count(new PurchaseItemCriteria()));
	}

	@Test
	public void deleteByUserId_noResults() {
		int deleted = repo().delete(new PurchaseItemCriteria()
				.withUserId(DBReset.FULANO_ID));
		assertEquals(0, deleted);
		assertEquals(3, repo().count(new PurchaseItemCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new PurchaseItemCriteria()
				.withPurchaseItemId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}
}
