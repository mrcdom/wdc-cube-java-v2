package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.utils.ProjectionValues;
import br.com.wdc.shopping.scripts.sgbd.DBReset;
import br.com.wdc.shopping.test.util.BaseBusinessTest;

public class PurchaseRepositoryTest extends BaseBusinessTest {

	private PurchaseRepository repo() {
		return PurchaseRepository.BEAN.get();
	}

	private Purchase purchaseProjectionWithUser() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new Purchase();
		prj.id = pv.i64;
		prj.buyDate = pv.offsetDateTime;
		prj.user = new User();
		prj.user.id = pv.i64;
		return prj;
	}

	// :: fetch

	@Test
	public void fetchAll_returnsSeededPurchases() {
		List<Purchase> purchases = repo().fetch(new PurchaseCriteria());
		assertEquals(2, purchases.size());
	}

	@Test
	public void fetchById_returnsCorrectPurchase() {
		var purchase = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, purchaseProjectionWithUser());
		assertNotNull(purchase);
		assertNotNull(purchase.buyDate);
		assertNotNull(purchase.user);
		assertEquals(DBReset.ADMIN_ID, purchase.user.id);
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var purchase = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(purchase);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Purchase();
		projection.id = pv.i64;
		projection.buyDate = pv.offsetDateTime;

		var purchase = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, projection);
		assertNotNull(purchase);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, purchase.id);
		assertNotNull(purchase.buyDate);
	}

	@Test
	public void fetchWithProjectionList_filterItemsByCriteria() {
		var pv = ProjectionValues.INSTANCE;

		// Projeção do item: apenas id, amount e product.id
		var itemPrj = new PurchaseItem();
		itemPrj.id = pv.i64;
		itemPrj.amount = pv.i32;
		itemPrj.product = new Product();
		itemPrj.product.id = pv.i64;

		// Critério para filtrar apenas items do produto BOLA_WILSON
		var itemCriteria = new PurchaseItemCriteria()
				.withProductId(DBReset.BOLA_WILSON_ID);

		// Projeção da compra com items via ProjectionList (aplica critério na sub-coleção)
		var projection = new Purchase();
		projection.id = pv.i64;
		projection.items = pv.singletonList(itemPrj, itemCriteria);

		// ADMIN_SECOND_PURCHASE tem 2 items: BOLA_WILSON e FITA_VEDA_ROSCA
		// Com o critério, deve retornar apenas o item BOLA_WILSON
		var purchase = repo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ID, projection);
		assertNotNull(purchase);
		assertNotNull(purchase.items);
		assertEquals(1, purchase.items.size());
		assertEquals(DBReset.BOLA_WILSON_ID, purchase.items.get(0).product.id);
	}

	@Test
	public void fetchByUserId() {
		var criteria = new PurchaseCriteria()
				.withUserId(DBReset.ADMIN_ID)
				.withProjection(purchaseProjectionWithUser());
		var purchases = repo().fetch(criteria);
		assertEquals(2, purchases.size());
		for (var p : purchases) {
			assertEquals(DBReset.ADMIN_ID, p.user.id);
		}
	}

	@Test
	public void fetchByUserId_noResults() {
		var purchases = repo().fetch(new PurchaseCriteria().withUserId(DBReset.FULANO_ID));
		assertTrue(purchases.isEmpty());
	}

	@Test
	public void fetchByPurchaseId() {
		var purchases = repo().fetch(new PurchaseCriteria().withPurchaseId(DBReset.ADMIN_SECOND_PURCHASE_ID));
		assertEquals(1, purchases.size());
		assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, purchases.get(0).id);
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.ACENDING)
				.withOffset(0)
				.withLimit(1));
		assertEquals(1, purchases.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.ACENDING));
		assertEquals(2, purchases.size());
		assertTrue(purchases.get(0).id <= purchases.get(1).id);
	}

	@Test
	public void fetchWithOrderDescending() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.DESCENDING));
		assertEquals(2, purchases.size());
		assertTrue(purchases.get(0).id >= purchases.get(1).id);
	}

	// :: count

	@Test
	public void countAll_returnsTwo() {
		int count = repo().count(new PurchaseCriteria());
		assertEquals(2, count);
	}

	@Test
	public void countByUserId() {
		int count = repo().count(new PurchaseCriteria().withUserId(DBReset.ADMIN_ID));
		assertEquals(2, count);
	}

	@Test
	public void countNonExistent_returnsZero() {
		int count = repo().count(new PurchaseCriteria().withPurchaseId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: insert

	@Test
	public void insert_newPurchase() {
		var purchase = new Purchase();
		purchase.buyDate = OffsetDateTime.now();
		purchase.user = new User();
		purchase.user.id = DBReset.FULANO_ID;

		boolean inserted = repo().insert(purchase);
		assertTrue(inserted);
		assertNotNull(purchase.id);

		var fetched = repo().fetchById(purchase.id, purchaseProjectionWithUser());
		assertNotNull(fetched);
		assertEquals(DBReset.FULANO_ID, fetched.user.id);
	}

	// :: update

	@Test
	public void update_existingPurchase() {
		var prj = purchaseProjectionWithUser();
		var original = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, prj);
		assertNotNull(original);

		var updated = new Purchase();
		updated.id = original.id;
		updated.buyDate = OffsetDateTime.now();
		updated.user = new User();
		updated.user.id = DBReset.BEOTRANO_ID;

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, prj);
		assertEquals(DBReset.BEOTRANO_ID, fetched.user.id);
	}

	// :: insertOrUpdate

	@Test
	public void insertOrUpdate_insertsWhenNew() {
		var purchase = new Purchase();
		purchase.buyDate = OffsetDateTime.now();
		purchase.user = new User();
		purchase.user.id = DBReset.BEOTRANO_ID;

		boolean result = repo().insertOrUpdate(purchase);
		assertTrue(result);
		assertNotNull(purchase.id);

		var fetched = repo().fetchById(purchase.id, purchaseProjectionWithUser());
		assertNotNull(fetched);
		assertEquals(DBReset.BEOTRANO_ID, fetched.user.id);
	}

	@Test
	public void insertOrUpdate_updatesWhenExisting() {
		var purchase = new Purchase();
		purchase.id = DBReset.ADMIN_FIRST_PURCHASE_ID;
		purchase.buyDate = OffsetDateTime.now();
		purchase.user = new User();
		purchase.user.id = DBReset.FULANO_ID;

		boolean result = repo().insertOrUpdate(purchase);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, purchaseProjectionWithUser());
		assertEquals(DBReset.FULANO_ID, fetched.user.id);
	}

	// :: delete

	@Test
	public void deleteByPurchaseId() {
		// First delete purchase items to avoid FK constraint
		PurchaseItemRepository.BEAN.get().delete(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));

		int deleted = repo().delete(new PurchaseCriteria().withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));
		assertEquals(1, deleted);
		assertEquals(1, repo().count(new PurchaseCriteria()));
	}

	@Test
	public void deleteByUserId() {
		// First delete all purchase items to avoid FK constraint
		PurchaseItemRepository.BEAN.get().delete(new PurchaseItemCriteria()
				.withUserId(DBReset.ADMIN_ID));

		int deleted = repo().delete(new PurchaseCriteria().withUserId(DBReset.ADMIN_ID));
		assertEquals(2, deleted);
		assertEquals(0, repo().count(new PurchaseCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new PurchaseCriteria().withPurchaseId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}
}
