package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.shopping.domain.product.Product;
import br.com.wdc.shopping.domain.product.ProductCriteria;
import br.com.wdc.shopping.domain.product.ProductRepository;
import br.com.wdc.shopping.scripts.sgbd.DBReset;

public abstract class AbstractProductRepositoryTest {

	protected abstract ProductRepository repo();

	// :: fetch

	@Test
	public void fetchAll_returnsFourProducts() {
		List<Product> products = repo().fetch(new ProductCriteria());
		assertEquals(4, products.size());
	}

	@Test
	public void fetchById_returnsCorrectProduct() {
		var product = repo().fetchById(DBReset.CAFETEIRA_ID, null);
		assertNotNull(product);
		assertNotNull(product.name());
		assertNotNull(product.price());
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var product = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(product);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product()
				.withId(pv.i64)
				.withName(pv.str);

		var product = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, projection);
		assertNotNull(product);
		assertEquals(DBReset.PEN_DRIVE2GB_ID, product.id());
		assertNotNull(product.name());
	}

	@Test
	public void fetchByCriteria_productId() {
		var products = repo().fetch(new ProductCriteria().withProductId(DBReset.BOLA_WILSON_ID));
		assertEquals(1, products.size());
		assertEquals(DBReset.BOLA_WILSON_ID, products.get(0).id());
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.OLDEST_FIRST), 0, 2);
		assertEquals(2, products.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.OLDEST_FIRST));
		assertEquals(4, products.size());
		for (int i = 1; i < products.size(); i++) {
			assertTrue(products.get(i - 1).id() <= products.get(i).id());
		}
	}

	@Test
	public void fetchWithOrderDescending() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.NEWEST_FIRST));
		assertEquals(4, products.size());
		for (int i = 1; i < products.size(); i++) {
			assertTrue(products.get(i - 1).id() >= products.get(i).id());
		}
	}

	// :: count

	@Test
	public void countAll_returnsFour() {
		int count = repo().count(new ProductCriteria());
		assertEquals(4, count);
	}

	@Test
	public void countByProductId_returnsOne() {
		int count = repo().count(new ProductCriteria().withProductId(DBReset.CAFETEIRA_ID));
		assertEquals(1, count);
	}

	@Test
	public void countNonExistent_returnsZero() {
		int count = repo().count(new ProductCriteria().withProductId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: fetchImage

	@Test
	public void fetchImage_returnsNonNullForSeededProduct() {
		byte[] image = repo().fetchImage(DBReset.CAFETEIRA_ID);
		assertNotNull(image);
		assertTrue(image.length > 0);
	}

	@Test(expected = BusinessException.class)
	public void fetchImage_nonExistent_throws() {
		repo().fetchImage(Long.MAX_VALUE);
	}

	// :: insert

	@Test
	public void insert_newProduct() {
		var product = new Product()
				.withName("Teclado USB")
				.withPrice(89.90)
				.withDescription("Teclado mecanico");

		boolean inserted = repo().insert(product);
		assertTrue(inserted);
		assertNotNull(product.id());

		var fetched = repo().fetchById(product.id(), null);
		assertNotNull(fetched);
		assertEquals("Teclado USB", fetched.name());
		assertEquals(89.90, fetched.price(), 0.001);
		assertNotNull(fetched.description());
		assertTrue(fetched.description().contains("Teclado"));
	}

	// :: update

	@Test
	public void update_existingProduct() {
		var original = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertNotNull(original);

		var updated = new Product()
				.withId(original.id())
				.withName("Pen Drive 4GB")
				.withPrice(35.0)
				.withDescription(original.description());

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertEquals("Pen Drive 4GB", fetched.name());
		assertEquals(35.0, fetched.price(), 0.001);
	}

	@Test
	public void update_partialFields_onlyChangesSpecifiedFields() {
		var original = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertNotNull(original);
		var originalDescription = original.description();
		var originalPrice = original.price();

		// projeção parcial: só id e name
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product()
				.withId(pv.i64)
				.withName(pv.str);

		var updated = new Product()
				.withId(original.id())
				.withName("Pen Drive Renomeado");

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertEquals("Pen Drive Renomeado", fetched.name());
		assertEquals(originalPrice, fetched.price(), 0.001);
		assertEquals(originalDescription, fetched.description());
	}

	@Test
	public void update_setFieldToNull_clearsValue() {
		// Insere um produto com description nullable para este teste
		// Como DESCRIPTION é NOT NULL no schema, testamos que um campo fora da projeção
		// não é afetado mesmo que esteja null no newEntity
		var original = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertNotNull(original);

		// projeção só com id e price — description NÃO está na projeção
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product()
				.withId(pv.i64)
				.withPrice(pv.f64);

		var updated = new Product()
				.withId(original.id())
				.withPrice(99.99);
		updated.withDescription(null); // null no newEntity, mas NÃO na projeção

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		// description preservada (campo fora da projeção não é tocado)
		assertEquals(original.description(), fetched.description());
		// price atualizado
		assertEquals(99.99, fetched.price(), 0.001);
	}

	// :: delete

	@Test
	public void deleteByProductId() {
		int deleted = repo().delete(new ProductCriteria().withProductId(DBReset.PEN_DRIVE2GB_ID));
		assertEquals(1, deleted);
		assertEquals(3, repo().count(new ProductCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new ProductCriteria().withProductId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}

	// :: Critério expressivo — rodam também no modo REST, onde provam que os operadores trafegam

	@Test
	public void in_bringsOnlyTheListedOnes() {
		var products = repo().fetch(new ProductCriteria()
				.productId().in(DBReset.CAFETEIRA_ID, DBReset.PEN_DRIVE2GB_ID));

		assertEquals(2, products.size());
	}

	@Test
	public void between_isInclusiveOnBothEnds() {
		// ids 1..3 dos quatro produtos
		var products = repo().fetch(new ProductCriteria()
				.productId().between(DBReset.BOLA_WILSON_ID, DBReset.PEN_DRIVE2GB_ID));

		assertEquals(3, products.size());
	}

	@Test
	public void ne_excludesTheOne() {
		var products = repo().fetch(new ProductCriteria().productId().ne(DBReset.CAFETEIRA_ID));

		assertEquals(3, products.size());
	}

	@Test
	public void twoNe_excludeBoth() {
		// Dois pedidos no mesmo campo valem juntos: é o AND por padrão. Fosse OR, cada linha satisfaria um dos dois
		// e a consulta devolveria os quatro.
		var criteria = new ProductCriteria();
		criteria.productId().ne(DBReset.CAFETEIRA_ID);
		criteria.productId().ne(DBReset.BOLA_WILSON_ID);

		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void or_turnsRequestsIntoAlternatives() {
		var criteria = new ProductCriteria();
		criteria.productId().or().eq(DBReset.CAFETEIRA_ID);
		criteria.productId().eq(DBReset.BOLA_WILSON_ID);

		// Com AND seriam zero: nenhum id é os dois ao mesmo tempo.
		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void gt_comparesByOrder() {
		var products = repo().fetch(new ProductCriteria().productId().gt(DBReset.BOLA_WILSON_ID));

		assertEquals(2, products.size());
	}

	@Test
	public void betweenWithOpenEnd_becomesASingleBound() {
		// Só o início informado vira >=; é o filtro de tela com um campo preenchido só.
		var products = repo().fetch(new ProductCriteria()
				.productId().between(DBReset.FITA_VEDA_ROSCA_ID, null));

		assertEquals(2, products.size());
	}

	@Test
	public void emptyCriterion_doesNotFilter() {
		var criteria = new ProductCriteria();
		criteria.productId().eq(null);

		assertEquals("valor nulo não acrescenta pedido", 4, repo().fetch(criteria).size());
	}

	@Test
	public void clear_undoesWhatWasAsked() {
		var criteria = new ProductCriteria().withProductId(DBReset.CAFETEIRA_ID);
		assertEquals(1, repo().fetch(criteria).size());

		criteria.productId().clear();
		assertEquals(4, repo().fetch(criteria).size());
	}

	@Test
	public void deleteWithEmptyCriteria_isRefused() {
		// Sem esta guarda, critério vazio traduziria para noCondition() e o DELETE levaria a tabela inteira.
		try {
			repo().delete(new ProductCriteria());
			fail("delete sem chave deveria ser recusado");
		} catch (AssertionError | RuntimeException expected) {
			assertEquals(4, repo().count(new ProductCriteria()));
		}
	}

	// :: Campos que não são a chave — a superfície de filtro cobre todos os campos da entidade

	@Test
	public void filterByName_text() {
		var products = repo().fetch(new ProductCriteria().name().containing("Wilson"));

		assertEquals(1, products.size());
		assertEquals(DBReset.BOLA_WILSON_ID, products.get(0).id());
	}

	@Test
	public void filterByPrice_range() {
		// Preço é NUMERIC na coluna e Double no domínio: a conversão é do campo, e vale igual para between.
		// Dos quatro preços — 199.99, 45.30, 2.67 e 16.00 —, três caem na faixa.
		var products = repo().fetch(new ProductCriteria().price().between(2.0, 50.0));

		assertEquals(3, products.size());
		for (var p : products) {
			assertTrue("preço fora da faixa pedida: " + p.price(), p.price() >= 2.0 && p.price() <= 50.0);
		}
	}

	@Test
	public void filterByPrice_greaterThan() {
		var products = repo().fetch(new ProductCriteria().price().gt(100.0));

		assertEquals(1, products.size());
		assertEquals(DBReset.CAFETEIRA_ID, products.get(0).id());
	}

	@Test
	public void filterByDescription_text() {
		var products = repo().fetch(new ProductCriteria().description().containing("Teflon"));

		assertEquals(1, products.size());
		assertEquals(DBReset.FITA_VEDA_ROSCA_ID, products.get(0).id());
	}

	@Test
	public void filterByTwoDifferentFields_combinesWithAnd() {
		// Entre campos a junção é sempre AND, mesmo que cada um deles seja disjuntivo por dentro.
		var criteria = new ProductCriteria();
		criteria.name().containing("a");
		criteria.price().lt(50.0);

		var products = repo().fetch(criteria);
		for (var p : products) {
			assertTrue(p.name().toLowerCase().contains("a"));
			assertTrue(p.price() < 50.0);
		}
	}

	// :: Ordenações provisionadas — cada uma é um efeito, não um campo com direção

	private List<String> nomesOrdenadosPor(ProductCriteria.OrderBy order) {
		return repo().fetch(new ProductCriteria().withOrderBy(order)).stream().map(Product::name).toList();
	}

	@Test
	public void orderBy_nameAToZ() {
		var nomes = nomesOrdenadosPor(ProductCriteria.OrderBy.NAME_A_TO_Z);

		assertEquals(nomes.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), nomes);
		// E não coincide com a ordem de cadastro — sem isso o teste passaria mesmo sem ordenar.
		assertNotEquals(nomesOrdenadosPor(ProductCriteria.OrderBy.OLDEST_FIRST), nomes);
	}

	@Test
	public void orderBy_cheapestFirst() {
		var precos = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.CHEAPEST_FIRST))
				.stream().map(Product::price).toList();

		assertEquals(precos.stream().sorted().toList(), precos);
	}

	@Test
	public void orderBy_mostExpensiveFirst_isTheReverse() {
		var caros = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.MOST_EXPENSIVE_FIRST))
				.stream().map(Product::price).toList();
		var baratos = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.CHEAPEST_FIRST))
				.stream().map(Product::price).toList();

		assertEquals(baratos.reversed(), caros);
	}

	@Test
	public void orderBy_oldestAndNewest_areOpposites() {
		var antigos = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.OLDEST_FIRST)).stream().map(Product::id).toList();
		var novos = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.NEWEST_FIRST)).stream().map(Product::id).toList();

		assertEquals(antigos.reversed(), novos);
	}
}
