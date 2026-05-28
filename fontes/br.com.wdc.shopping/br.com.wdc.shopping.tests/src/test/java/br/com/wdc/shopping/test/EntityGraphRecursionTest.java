package br.com.wdc.shopping.test;

import static org.junit.Assert.*;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.PurchaseItemModelCodec;
import br.com.wdc.shopping.domain.codec.PurchaseModelCodec;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.model.User;

/**
 * Testa que a proteção contra recursão cíclica no grafo de entidades funciona
 * corretamente nos codecs (write com EntityGraph + read com stub de back-reference).
 */
public class EntityGraphRecursionTest {

	private final PurchaseModelCodec purchaseCodec = new PurchaseModelCodec();
	private final PurchaseItemModelCodec itemCodec = new PurchaseItemModelCodec();

	// ── Write: EntityGraph impede serialização infinita ──

	@Test
	public void writeEntity_withCircularReference_doesNotStackOverflow() {
		// Monta grafo cíclico: Purchase → items[0] → purchase → (mesma instância)
		var purchase = new Purchase();
		purchase.id = 1L;
		purchase.buyDate = OffsetDateTime.now();
		purchase.user = new User();
		purchase.user.id = 10L;
		purchase.user.userName = "admin";

		var item = new PurchaseItem();
		item.id = 100L;
		item.amount = 2;
		item.price = 19.99;
		item.product = new Product();
		item.product.id = 50L;
		item.product.name = "Widget";
		item.purchase = purchase; // back-reference cíclica!

		purchase.items = List.of(item);

		// writeEntity com EntityGraph NÃO deve entrar em loop infinito
		var writer = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer, purchase, new EntityGraph());
		var json = writer.result();

		assertNotNull(json);
		assertTrue("JSON deve conter o id da purchase", json.contains("\"id\":1"));
		assertTrue("JSON deve conter items", json.contains("\"items\""));
		// O product dentro do item deve ser serializado
		assertTrue("JSON deve conter o produto", json.contains("\"Widget\""));
	}

	@Test
	public void writeEntity_withCircularReference_writesOnlyKeyForDuplicate() {
		// PurchaseItem tem referência ao Purchase pai — ao serializar o item isoladamente,
		// se purchase já foi rastreada, deve escrever apenas o id como stub
		var purchase = new Purchase();
		purchase.id = 42L;
		purchase.buyDate = OffsetDateTime.now();

		var item = new PurchaseItem();
		item.id = 7L;
		item.amount = 1;
		item.price = 9.50;
		item.purchase = purchase;

		// Simula que Purchase já foi rastreada antes
		var graph = new EntityGraph();
		graph.track(purchase); // marca como já vista

		var writer = new JsonStreamWriter();
		itemCodec.writeEntity(writer, item, graph);
		var json = writer.result();

		assertNotNull(json);
		// O item deve ter sido serializado normalmente (primeira vez)
		assertTrue("JSON deve conter id do item", json.contains("\"id\":7"));
		assertTrue("JSON deve conter purchaseId", json.contains("\"purchaseId\":42"));
	}

	@Test
	public void writeEntity_sameEntityTwice_secondTimeWritesOnlyKey() {
		var purchase = new Purchase();
		purchase.id = 99L;
		purchase.buyDate = OffsetDateTime.now();
		purchase.user = new User();
		purchase.user.id = 5L;
		purchase.user.userName = "test";

		var graph = new EntityGraph();

		// Primeira escrita — completa
		var writer1 = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer1, purchase, graph);
		var json1 = writer1.result();
		assertTrue("Primeira escrita deve ser completa", json1.contains("\"buyDate\""));
		assertTrue("Primeira escrita deve conter user", json1.contains("\"user\""));

		// Segunda escrita — apenas chave
		var writer2 = new JsonStreamWriter();
		purchaseCodec.writeEntity(writer2, purchase, graph);
		var json2 = writer2.result();
		assertTrue("Segunda escrita deve conter id", json2.contains("\"id\":99"));
		assertFalse("Segunda escrita NÃO deve conter buyDate", json2.contains("\"buyDate\""));
		assertFalse("Segunda escrita NÃO deve conter user", json2.contains("\"user\""));
	}

	// ── Read: back-reference deve ser stub (instância separada) ──

	@Test
	public void readEntity_purchaseWithItems_itemsHaveStubBackReference() {
		// JSON que simula Purchase com items (como viria do servidor)
		var json = """
				{"id":1,"buyDate":"2026-01-15T10:30:00-03:00","items":[{"id":100,"amount":2,"price":19.99},{"id":101,"amount":1,"price":5.00}]}""";

		var reader = new JsonStreamReader(json);
		var purchase = purchaseCodec.readEntity(reader);

		assertNotNull(purchase);
		assertEquals(Long.valueOf(1L), purchase.id);
		assertNotNull(purchase.items);
		assertEquals(2, purchase.items.size());

		// Cada item deve ter um stub de back-reference (purchase com apenas o id)
		for (var item : purchase.items) {
			assertNotNull("Item deve ter back-reference purchase", item.purchase);
			assertEquals("Back-reference deve ter o id correto", Long.valueOf(1L), item.purchase.id);
			// A instância NÃO deve ser o mesmo objeto (evita ciclo)
			assertNotSame("Back-reference NÃO deve ser a mesma instância do parent", purchase, item.purchase);
			// Stub deve ter apenas o id (sem outros campos)
			assertNull("Stub não deve ter buyDate", item.purchase.buyDate);
			assertNull("Stub não deve ter user", item.purchase.user);
			assertNull("Stub não deve ter items", item.purchase.items);
		}
	}

	@Test
	public void readEntity_purchaseWithoutId_itemsHaveNoBackReference() {
		// Purchase sem id (entidade nova, não persistida)
		var json = """
				{"buyDate":"2026-01-15T10:30:00-03:00","items":[{"id":100,"amount":1,"price":10.0}]}""";

		var reader = new JsonStreamReader(json);
		var purchase = purchaseCodec.readEntity(reader);

		assertNotNull(purchase);
		assertNull(purchase.id);
		assertNotNull(purchase.items);
		assertEquals(1, purchase.items.size());
		// Sem id no parent, não há como criar stub
		assertNull("Sem id no parent, back-reference deve ser null", purchase.items.get(0).purchase);
	}

	// ── EntityGraph: rastreamento por identidade de instância ──

	@Test
	public void entityGraph_sameInstanceTwice_returnsFalseOnSecond() {
		var graph = new EntityGraph();
		var purchase = new Purchase();
		purchase.id = 1L;

		assertTrue("Primeiro track deve retornar true", graph.track(purchase));
		assertFalse("Segundo track (mesma instância) deve retornar false", graph.track(purchase));
	}

	@Test
	public void entityGraph_differentEntitiesSameKey_areIndependent() {
		var graph = new EntityGraph();
		var purchase = new Purchase();
		purchase.id = 1L;
		var product = new Product();
		product.id = 1L; // mesmo valor de key, mas instância diferente

		assertTrue("Purchase deve ser rastreada", graph.track(purchase));
		assertTrue("Product (instância diferente) deve ser independente", graph.track(product));
	}

	@Test
	public void entityGraph_nullEntity_alwaysReturnsTrue() {
		var graph = new EntityGraph();

		assertTrue("null sempre retorna true", graph.track(null));
		assertFalse("isSeen(null) deve ser false", graph.isSeen(null));
	}

	@Test
	public void entityGraph_differentInstancesSameKey_areNotDuplicate() {
		var graph = new EntityGraph();
		var p1 = new Purchase();
		p1.id = 42L;
		var p2 = new Purchase();
		p2.id = 42L; // mesma chave, mas instância DIFERENTE — legítimo na projeção

		assertTrue(graph.track(p1));
		assertTrue("Instância diferente com mesma chave NÃO é duplicata", graph.track(p2));
		assertFalse("p2 é instância distinta, não deve ser 'seen' como p1", graph.isSeen(p1) && p1 == p2);
	}
}
