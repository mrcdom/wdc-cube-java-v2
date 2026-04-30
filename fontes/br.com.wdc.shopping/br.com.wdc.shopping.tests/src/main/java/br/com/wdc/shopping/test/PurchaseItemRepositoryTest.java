package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.BaseBusinessTest;

public class PurchaseItemRepositoryTest extends BaseBusinessTest {

	private PurchaseItemRepository repo() {
		return PurchaseItemRepository.BEAN.get();
	}

	private PurchaseItem projectionWithRelations() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new PurchaseItem();
		prj.id = pv.i64;
		prj.amount = pv.i32;
		prj.price = pv.f64;
		prj.purchase = new Purchase();
		prj.purchase.id = pv.i64;
		prj.product = new Product();
		prj.product.id = pv.i64;
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
		assertNotNull(item.amount);
		assertNotNull(item.price);
		assertNotNull(item.purchase);
		assertNotNull(item.product);
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var item = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(item);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new PurchaseItem();
		projection.id = pv.i64;
		projection.amount = pv.i32;
		projection.price = pv.f64;

		var item = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, projection);
		assertNotNull(item);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, item.id);
		assertNotNull(item.amount);
		assertNotNull(item.price);
	}

	@Test
	public void fetchByPurchaseId_firstPurchase() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));
		assertEquals(1, items.size());
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, items.get(0).id);
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
			assertEquals(DBReset.CAFETEIRA_ID, item.product.id);
		}
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.ACENDING)
				.withOffset(0)
				.withLimit(2));
		assertEquals(2, items.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.ACENDING));
		assertEquals(3, items.size());
		for (int i = 1; i < items.size(); i++) {
			assertTrue(items.get(i - 1).id <= items.get(i).id);
		}
	}

	@Test
	public void fetchWithOrderDescending() {
		var items = repo().fetch(new PurchaseItemCriteria()
				.withOrderBy(PurchaseItemCriteria.OrderBy.DESCENDING));
		assertEquals(3, items.size());
		for (int i = 1; i < items.size(); i++) {
			assertTrue(items.get(i - 1).id >= items.get(i).id);
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
		var item = new PurchaseItem();
		item.amount = 5;
		item.price = 15.50;
		item.purchase = new Purchase();
		item.purchase.id = DBReset.ADMIN_FIRST_PURCHASE_ID;
		item.product = new Product();
		item.product.id = DBReset.PEN_DRIVE2GB_ID;

		boolean inserted = repo().insert(item);
		assertTrue(inserted);
		assertNotNull(item.id);

		var fetched = repo().fetchById(item.id, projectionWithRelations());
		assertNotNull(fetched);
		assertEquals(Integer.valueOf(5), fetched.amount);
		assertEquals(15.50, fetched.price, 0.001);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, fetched.purchase.id);
		assertEquals(DBReset.PEN_DRIVE2GB_ID, fetched.product.id);
	}

	// :: update

	@Test
	public void update_existingPurchaseItem() {
		var original = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, null);
		assertNotNull(original);

		var updated = new PurchaseItem();
		updated.id = original.id;
		updated.amount = 99;
		updated.price = 999.99;
		updated.purchase = original.purchase;
		updated.product = original.product;

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ITEM0_ID, null);
		assertEquals(Integer.valueOf(99), fetched.amount);
		assertEquals(999.99, fetched.price, 0.001);
	}

	// :: insertOrUpdate

	@Test
	public void insertOrUpdate_insertsWhenNew() {
		var item = new PurchaseItem();
		item.amount = 3;
		item.price = 25.0;
		item.purchase = new Purchase();
		item.purchase.id = DBReset.ADMIN_SECOND_PURCHASE_ID;
		item.product = new Product();
		item.product.id = DBReset.BOLA_WILSON_ID;

		boolean result = repo().insertOrUpdate(item);
		assertTrue(result);
		assertNotNull(item.id);

		assertEquals(4, repo().count(new PurchaseItemCriteria()));
	}

	@Test
	public void insertOrUpdate_updatesWhenExisting() {
		var item = new PurchaseItem();
		item.id = DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID;
		item.amount = 77;
		item.price = 77.77;
		item.purchase = new Purchase();
		item.purchase.id = DBReset.ADMIN_SECOND_PURCHASE_ID;
		item.product = new Product();
		item.product.id = DBReset.CAFETEIRA_ID;

		boolean result = repo().insertOrUpdate(item);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, null);
		assertEquals(Integer.valueOf(77), fetched.amount);
		assertEquals(77.77, fetched.price, 0.001);
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
		// userId não é coluna de EN_PURCHASEITEM — o delete usa EXISTS com subquery em EN_PURCHASE
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
