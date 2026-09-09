package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.domain.projection.ProjectionList;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseCriteria;
import br.com.wdc.shopping.domain.purchase.PurchaseRepository;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemRepository;
import br.com.wdc.shopping.domain.user.User;
import br.com.wdc.shopping.scripts.sgbd.DBReset;

public abstract class AbstractPurchaseRepositoryTest {

	protected abstract PurchaseRepository repo();

	protected abstract PurchaseItemRepository purchaseItemRepo();

	private Purchase purchaseProjectionWithUser() {
		var pv = ProjectionValues.INSTANCE;
		var prj = new Purchase()
				.withId(pv.i64)
				.withBuyDate(pv.offsetDateTime)
				.withUser(new User().withId(pv.i64));
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
		assertNotNull(purchase.buyDate());
		assertNotNull(purchase.user());
		assertEquals(DBReset.ADMIN_ID, purchase.user().id());
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var purchase = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(purchase);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Purchase()
				.withId(pv.i64)
				.withBuyDate(pv.offsetDateTime);

		var purchase = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, projection);
		assertNotNull(purchase);
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, purchase.id());
		assertNotNull(purchase.buyDate());
	}

	@Test
	public void fetchByUserId() {
		var criteria = new PurchaseCriteria()
				.withUserId(DBReset.ADMIN_ID)
				.withProjection(purchaseProjectionWithUser());
		var purchases = repo().fetch(criteria);
		assertEquals(2, purchases.size());
		for (var p : purchases) {
			assertEquals(DBReset.ADMIN_ID, p.user().id());
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
		assertEquals(DBReset.ADMIN_SECOND_PURCHASE_ID, purchases.get(0).id());
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.ASCENDING), 0, 1);
		assertEquals(1, purchases.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.ASCENDING));
		assertEquals(2, purchases.size());
		assertTrue(purchases.get(0).id() <= purchases.get(1).id());
	}

	@Test
	public void fetchWithOrderDescending() {
		var purchases = repo().fetch(new PurchaseCriteria()
				.withOrderBy(PurchaseCriteria.OrderBy.DESCENDING));
		assertEquals(2, purchases.size());
		assertTrue(purchases.get(0).id() >= purchases.get(1).id());
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
		var purchase = new Purchase()
				.withBuyDate(OffsetDateTime.now())
				.withUser(new User().withId(DBReset.FULANO_ID));

		boolean inserted = repo().insert(purchase);
		assertTrue(inserted);
		assertNotNull(purchase.id());

		var fetched = repo().fetchById(purchase.id(), purchaseProjectionWithUser());
		assertNotNull(fetched);
		assertEquals(DBReset.FULANO_ID, fetched.user().id());
	}

	// :: update

	@Test
	public void update_existingPurchase() {
		var prj = purchaseProjectionWithUser();
		var original = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, prj);
		assertNotNull(original);

		var updated = new Purchase()
				.withId(original.id())
				.withBuyDate(OffsetDateTime.now())
				.withUser(new User().withId(DBReset.BEOTRANO_ID));

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.ADMIN_FIRST_PURCHASE_ID, prj);
		assertEquals(DBReset.BEOTRANO_ID, fetched.user().id());
	}

	// :: delete

	@Test
	public void deleteByPurchaseId() {
		// First delete purchase items to avoid FK constraint
		purchaseItemRepo().delete(new PurchaseItemCriteria()
				.withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));

		int deleted = repo().delete(new PurchaseCriteria().withPurchaseId(DBReset.ADMIN_FIRST_PURCHASE_ID));
		assertEquals(1, deleted);
		assertEquals(1, repo().count(new PurchaseCriteria()));
	}

	@Test
	public void deleteByUserId() {
		purchaseItemRepo().delete(new PurchaseItemCriteria()
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

	// :: Coleção projetada — ordem, recorte e sub-critério da coleção filha.
	//    Nos Abstract, então rodam em LOCAL e em REST: o segundo prova que forma + critério + recorte
	//    da ProjectionList atravessam a serialização, não só a forma.

	private PurchaseItem itemProjection() {
		var pv = ProjectionValues.INSTANCE;
		return new PurchaseItem()
				.withId(pv.i64)
				.withAmount(pv.i32)
				.withProduct(new Product().withId(pv.i64));
	}

	private List<PurchaseItem> itemsOf(ProjectionList<PurchaseItem> items) {
		var pv = ProjectionValues.INSTANCE;
		var purchase = repo().fetchById(DBReset.ADMIN_SECOND_PURCHASE_ID,
				new Purchase().withId(pv.i64).withItems(items));
		assertNotNull(purchase);
		assertNotNull(purchase.items());
		return purchase.items();
	}

	private List<Long> idsOf(List<PurchaseItem> items) {
		return items.stream().map(PurchaseItem::id).toList();
	}

	private ProjectionList<PurchaseItem> items(PurchaseItemCriteria.OrderBy order) {
		var criteria = new PurchaseItemCriteria().withOrderBy(order);
		return ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria);
	}

	@Test
	public void collection_withoutOrderOrSlice_bringsEveryItem() {
		var items = ProjectionValues.INSTANCE.singletonList(itemProjection(), new PurchaseItemCriteria());
		assertEquals(2, itemsOf(items).size());
	}

	@Test
	public void collection_ascending_orders() {
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING))));
	}

	@Test
	public void collection_descending_reverses() {
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID, DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.DESCENDING))));
	}

	@Test
	public void collection_limit_cuts() {
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withLimit(1))));
	}

	@Test
	public void collection_limit_respectsOrder() {
		// Mesmo limite, ordem invertida: o item que sobra tem de ser o outro.
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.DESCENDING).withLimit(1))));
	}

	@Test
	public void collection_offset_skips() {
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(1))));
	}

	@Test
	public void collection_limitAndOffset() {
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM1_ID),
				idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(1).withLimit(1))));
	}

	@Test
	public void collection_offsetBeyondEnd_bringsNothing() {
		assertEquals(List.of(), idsOf(itemsOf(items(PurchaseItemCriteria.OrderBy.ASCENDING).withOffset(10))));
	}

	@Test
	public void collection_filtersBySubCriteria() {
		var criteria = new PurchaseItemCriteria().withProductId(DBReset.BOLA_WILSON_ID);
		var items = ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria);

		var result = itemsOf(items);
		assertEquals(1, result.size());
		assertEquals(DBReset.BOLA_WILSON_ID, result.get(0).product().id());
	}

	@Test
	public void collection_subCriteriaComposesWithSlice() {
		// O recorte repete o mesmo filtro da coleção — o corte cai sobre o conjunto já filtrado.
		var criteria = new PurchaseItemCriteria()
				.withProductId(DBReset.BOLA_WILSON_ID)
				.withOrderBy(PurchaseItemCriteria.OrderBy.ASCENDING);
		var items = ProjectionValues.INSTANCE.singletonList(itemProjection(), criteria).withLimit(5);

		var result = itemsOf(items);
		assertEquals(List.of(DBReset.ADMIN_SECOND_PURCHASE_ITEM0_ID), idsOf(result));
	}

	// :: Campos que não são a chave

	@Test
	public void filterByBuyDate_range() {
		// A coluna é TIMESTAMP sem fuso e o domínio fala em OffsetDateTime: a conversão acontece na tradução,
		// e o intervalo pedido aqui é o que chega ao banco.
		var de = OffsetDateTime.parse("2010-01-01T00:00:00Z");
		var ate = OffsetDateTime.parse("2011-12-31T23:59:59Z");

		var purchases = repo().fetch(new PurchaseCriteria().buyDate().between(de, ate));

		assertEquals("as duas compras do seed estão nesse intervalo", 2, purchases.size());
	}

	@Test
	public void filterByBuyDate_before() {
		var corte = OffsetDateTime.parse("2011-01-01T00:00:00Z");
		var purchases = repo().fetch(new PurchaseCriteria().buyDate().lt(corte));

		assertEquals(1, purchases.size());
		assertEquals(DBReset.ADMIN_FIRST_PURCHASE_ID, purchases.get(0).id());
	}
}
