package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.purchase.Purchase;
import br.com.wdc.shopping.domain.purchase.PurchaseCodec;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItem;
import br.com.wdc.shopping.domain.purchaseitem.PurchaseItemCodec;
import br.com.wdc.shopping.domain.user.User;

/**
 * Verifies that the codecs correctly guard against cyclic recursion in the entity
 * graph (write with EntityGraph + read with a back-reference stub).
 */
public class EntityGraphRecursionTest {

	private final PurchaseCodec purchaseCodec = new PurchaseCodec();
	private final PurchaseItemCodec itemCodec = new PurchaseItemCodec();

	// ── Write: EntityGraph prevents infinite serialization ──

	@Test
	public void writeEntity_withCircularReference_doesNotStackOverflow() {
		// Builds a cyclic graph: Purchase → items[0] → purchase → (same instance)
		var purchase = new Purchase()
				.withId(1L)
				.withBuyDate(OffsetDateTime.now())
				.withUser(new User()
		                .withId(10L)
		                .withUserName("admin"));

		var item = new PurchaseItem()
				.withId(100L)
				.withAmount(2)
				.withPrice(19.99)
				.withProduct(new Product()
		                .withId(50L)
		                .withName("Widget"));
		item.withPurchase(purchase); // cyclic back-reference!

		purchase.withItems(List.of(item));

		// writeEntity with EntityGraph must NOT loop forever
		var writer = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer, purchase, new EntityGraph());
		var json = writer.result();

		assertNotNull(json);
		assertTrue("JSON must contain the purchase id", json.contains("\"id\":1"));
		assertTrue("JSON must contain items", json.contains("\"items\""));
		// The product inside the item must be serialized
		assertTrue("JSON must contain the product", json.contains("\"Widget\""));
	}

	@Test
	public void writeEntity_withCircularReference_writesOnlyKeyForDuplicate() {
		// PurchaseItem references its parent Purchase — when the item is serialized on its own
		// and the purchase was already tracked, only the id is written as a stub
		var purchase = new Purchase()
				.withId(42L)
				.withBuyDate(OffsetDateTime.now());

		var item = new PurchaseItem()
				.withId(7L)
				.withAmount(1)
				.withPrice(9.50)
				.withPurchase(purchase);

		// Simulates a Purchase that was already tracked
		var graph = new EntityGraph();
		graph.track(purchase); // marks it as already seen

		var writer = new JsonStreamWriter();
		itemCodec.writeEntity(writer, item, graph);
		var json = writer.result();

		assertNotNull(json);
		// The item must have been serialized normally (first time)
		assertTrue("JSON must contain the item id", json.contains("\"id\":7"));
		assertTrue("JSON must contain purchaseId", json.contains("\"purchaseId\":42"));
	}

	@Test
	public void writeEntity_sameEntityTwice_secondTimeWritesOnlyKey() {
		var purchase = new Purchase()
				.withId(99L)
				.withBuyDate(OffsetDateTime.now())
				.withUser(new User()
		                .withId(5L)
		                .withUserName("test"));

		var graph = new EntityGraph();

		// First write — complete
		var writer1 = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer1, purchase, graph);
		var json1 = writer1.result();
		assertTrue("First write must be complete", json1.contains("\"buyDate\""));
		assertTrue("First write must contain user", json1.contains("\"user\""));

		// Second write — key only
		var writer2 = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer2, purchase, graph);
		var json2 = writer2.result();
		assertTrue("Second write must contain id", json2.contains("\"id\":99"));
		assertFalse("Second write must NOT contain buyDate", json2.contains("\"buyDate\""));
		assertFalse("Second write must NOT contain user", json2.contains("\"user\""));
	}

	// ── Read: the back-reference must be a stub (separate instance) ──

	@Test
	public void readEntity_purchaseWithItems_itemsHaveStubBackReference() {
		// JSON simulating a Purchase with items (as the server would send it)
		var json = """
				{"id":1,"buyDate":"2026-01-15T10:30:00-03:00","items":[{"id":100,"amount":2,"price":19.99},{"id":101,"amount":1,"price":5.00}]}""";

		var reader = new JsonStreamReader(json);
		var purchase = purchaseCodec.readEntity(reader);

		assertNotNull(purchase);
		assertEquals(Long.valueOf(1L), purchase.id());
		assertNotNull(purchase.items());
		assertEquals(2, purchase.items().size());

		// Each item must have a back-reference stub (purchase with only the id)
		for (var item : purchase.items()) {
			assertNotNull("Item must have a purchase back-reference", item.purchase());
			assertEquals("Back-reference must have the correct id", Long.valueOf(1L), item.purchase().id());
			// The instance must NOT be the same object (avoids a cycle)
			assertNotSame("Back-reference must NOT be the same instance as the parent", purchase, item.purchase());
			// The stub must have only the id (no other fields)
			assertNull("Stub must not have buyDate", item.purchase().buyDate());
			assertNull("Stub must not have user", item.purchase().user());
			assertNull("Stub must not have items", item.purchase().items());
		}
	}

	@Test
	public void readEntity_purchaseWithoutId_itemsHaveNoBackReference() {
		// Purchase without an id (new, unpersisted entity)
		var json = """
				{"buyDate":"2026-01-15T10:30:00-03:00","items":[{"id":100,"amount":1,"price":10.0}]}""";

		var reader = new JsonStreamReader(json);
		var purchase = purchaseCodec.readEntity(reader);

		assertNotNull(purchase);
		assertNull(purchase.id());
		assertNotNull(purchase.items());
		assertEquals(1, purchase.items().size());
		// Without a parent id there is no way to create a stub
		assertNull("Without a parent id, the back-reference must be null", purchase.items().get(0).purchase());
	}

	// ── EntityGraph: tracking by instance identity ──

	@Test
	public void entityGraph_sameInstanceTwice_returnsFalseOnSecond() {
		var graph = new EntityGraph();
		var purchase = new Purchase()
				.withId(1L);

		assertTrue("First track must return true", graph.track(purchase));
		assertFalse("Second track (same instance) must return false", graph.track(purchase));
	}

	@Test
	public void entityGraph_differentEntitiesSameKey_areIndependent() {
		var graph = new EntityGraph();
		var purchase = new Purchase()
				.withId(1L);
		var product = new Product();
		product.withId(1L); // same key value, but a different instance

		assertTrue("Purchase must be tracked", graph.track(purchase));
		assertTrue("Product (different instance) must be independent", graph.track(product));
	}

	@Test
	public void entityGraph_nullEntity_alwaysReturnsTrue() {
		var graph = new EntityGraph();

		assertTrue("null always returns true", graph.track(null));
		assertFalse("isSeen(null) must be false", graph.isSeen(null));
	}

	@Test
	public void entityGraph_differentInstancesSameKey_areNotDuplicate() {
		var graph = new EntityGraph();
		var p1 = new Purchase()
				.withId(42L);
		var p2 = new Purchase();
		p2.withId(42L); // same key, but a DIFFERENT instance — legitimate in a projection

		assertTrue(graph.track(p1));
		assertTrue("A different instance with the same key is NOT a duplicate", graph.track(p2));
		assertFalse("p2 is a distinct instance and must not be 'seen' as p1", graph.isSeen(p1) && p1 == p2);
	}
}
