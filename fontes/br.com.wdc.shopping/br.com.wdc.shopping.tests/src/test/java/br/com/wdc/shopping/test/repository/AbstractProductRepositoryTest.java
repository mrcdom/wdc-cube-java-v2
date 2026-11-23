package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
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
		var product = repo().fetchById(DBReset.COFFEE_MAKER_ID, null);
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

		var product = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, projection);
		assertNotNull(product);
		assertEquals(DBReset.FLASH_DRIVE_2GB_ID, product.id());
		assertNotNull(product.name());
	}

	@Test
	public void fetchByCriteria_productId() {
		var products = repo().fetch(new ProductCriteria()
				.withProductId(DBReset.WILSON_BALL_ID));
		assertEquals(1, products.size());
		assertEquals(DBReset.WILSON_BALL_ID, products.get(0).id());
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
		int count = repo().count(new ProductCriteria()
				.withProductId(DBReset.COFFEE_MAKER_ID));
		assertEquals(1, count);
	}

	@Test
	public void countNonExistent_returnsZero() {
		int count = repo().count(new ProductCriteria()
				.withProductId(Long.MAX_VALUE));
		assertEquals(0, count);
	}

	// :: fetchImage

	@Test
	public void fetchImage_returnsNonNullForSeededProduct() {
		byte[] image = repo().fetchImage(DBReset.COFFEE_MAKER_ID);
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
				.withName("USB Keyboard")
				.withPrice(89.90)
				.withDescription("Mechanical keyboard");

		boolean inserted = repo().insert(product);
		assertTrue(inserted);
		assertNotNull(product.id());

		var fetched = repo().fetchById(product.id(), null);
		assertNotNull(fetched);
		assertEquals("USB Keyboard", fetched.name());
		assertEquals(89.90, fetched.price(), 0.001);
		assertNotNull(fetched.description());
		assertTrue(fetched.description().contains("keyboard"));
	}

	// :: update

	@Test
	public void update_existingProduct() {
		var original = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		assertNotNull(original);

		var updated = new Product()
				.withId(original.id())
				.withName("4GB Flash Drive")
				.withPrice(35.0)
				.withDescription(original.description());

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		assertEquals("4GB Flash Drive", fetched.name());
		assertEquals(35.0, fetched.price(), 0.001);
	}

	@Test
	public void update_partialFields_onlyChangesSpecifiedFields() {
		var original = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		assertNotNull(original);
		var originalDescription = original.description();
		var originalPrice = original.price();

		// partial projection: only id and name
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product()
				.withId(pv.i64)
				.withName(pv.str);

		var updated = new Product()
				.withId(original.id())
				.withName("Renamed Flash Drive");

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		assertEquals("Renamed Flash Drive", fetched.name());
		assertEquals(originalPrice, fetched.price(), 0.001);
		assertEquals(originalDescription, fetched.description());
	}

	@Test
	public void update_setFieldToNull_clearsValue() {
		// DESCRIPTION is NOT NULL in the schema, so this test checks that a field outside the projection
		// is not affected even when it is null in newEntity
		var original = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		assertNotNull(original);

		// projection with only id and price — description is NOT in the projection
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product()
				.withId(pv.i64)
				.withPrice(pv.f64);

		var updated = new Product()
				.withId(original.id())
				.withPrice(99.99);
		updated.withDescription(null); // null in newEntity, but NOT in the projection

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.FLASH_DRIVE_2GB_ID, null);
		// description preserved (a field outside the projection is not touched)
		assertEquals(original.description(), fetched.description());
		// price updated
		assertEquals(99.99, fetched.price(), 0.001);
	}

	// :: delete

	@Test
	public void deleteByProductId() {
		int deleted = repo().delete(new ProductCriteria()
				.withProductId(DBReset.FLASH_DRIVE_2GB_ID));
		assertEquals(1, deleted);
		assertEquals(3, repo().count(new ProductCriteria()));
	}

	@Test
	public void deleteNonExistent_returnsZero() {
		int deleted = repo().delete(new ProductCriteria()
				.withProductId(Long.MAX_VALUE));
		assertEquals(0, deleted);
	}

	// :: Expressive criterion — these also run in REST mode, where they prove the operators travel over the wire

	@Test
	public void in_bringsOnlyTheListedOnes() {
		var products = repo().fetch(new ProductCriteria()
				.productId().in(DBReset.COFFEE_MAKER_ID, DBReset.FLASH_DRIVE_2GB_ID));

		assertEquals(2, products.size());
	}

	@Test
	public void between_isInclusiveOnBothEnds() {
		// ids 1..3 of the four products
		var products = repo().fetch(new ProductCriteria()
				.productId().between(DBReset.WILSON_BALL_ID, DBReset.FLASH_DRIVE_2GB_ID));

		assertEquals(3, products.size());
	}

	@Test
	public void ne_excludesTheOne() {
		var products = repo().fetch(new ProductCriteria().productId().ne(DBReset.COFFEE_MAKER_ID));

		assertEquals(3, products.size());
	}

	@Test
	public void twoNe_excludeBoth() {
		// Two requests on the same field apply together: AND is the default. With OR, every row would satisfy one of
		// them and the query would return all four.
		var criteria = new ProductCriteria();
		criteria.productId().ne(DBReset.COFFEE_MAKER_ID);
		criteria.productId().ne(DBReset.WILSON_BALL_ID);

		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void or_turnsRequestsIntoAlternatives() {
		var criteria = new ProductCriteria();
		criteria.productId().or().eq(DBReset.COFFEE_MAKER_ID);
		criteria.productId().eq(DBReset.WILSON_BALL_ID);

		// With AND it would be zero: no id is both at once.
		assertEquals(2, repo().fetch(criteria).size());
	}

	@Test
	public void gt_comparesByOrder() {
		var products = repo().fetch(new ProductCriteria().productId().gt(DBReset.WILSON_BALL_ID));

		assertEquals(2, products.size());
	}

	@Test
	public void betweenWithOpenEnd_becomesASingleBound() {
		// Only the given start becomes >=; it is the screen filter with just one field filled in.
		var products = repo().fetch(new ProductCriteria()
				.productId().between(DBReset.THREAD_SEAL_TAPE_ID, null));

		assertEquals(2, products.size());
	}

	@Test
	public void emptyCriterion_doesNotFilter() {
		var criteria = new ProductCriteria();
		criteria.productId().eq(null);

		assertEquals("null value adds no request", 4, repo().fetch(criteria).size());
	}

	@Test
	public void clear_undoesWhatWasAsked() {
		var criteria = new ProductCriteria()
				.withProductId(DBReset.COFFEE_MAKER_ID);
		assertEquals(1, repo().fetch(criteria).size());

		criteria.productId().clear();
		assertEquals(4, repo().fetch(criteria).size());
	}

	@Test
	public void deleteWithEmptyCriteria_isRefused() {
		// Without this guard, an empty criteria would translate to noCondition() and the DELETE would wipe the whole table.
		try {
			repo().delete(new ProductCriteria());
			fail("delete without a key should be refused");
		} catch (InvalidRequestException expected) {
			// Local or through the API (400): the same request refused, and nothing deleted
			assertEquals(4, repo().count(new ProductCriteria()));
		}
	}

	// :: Fields that are not the key — the filter surface covers every field of the entity

	@Test
	public void filterByName_text() {
		var products = repo().fetch(new ProductCriteria().name().containing("Wilson"));

		assertEquals(1, products.size());
		assertEquals(DBReset.WILSON_BALL_ID, products.get(0).id());
	}

	@Test
	public void filterByPrice_range() {
		// Price is NUMERIC in the column and Double in the domain: the conversion belongs to the field, and applies to between too.
		// Of the four prices — 199.99, 45.30, 2.67 and 16.00 — three fall within the range.
		var products = repo().fetch(new ProductCriteria().price().between(2.0, 50.0));

		assertEquals(3, products.size());
		for (var p : products) {
			assertTrue("price outside the requested range: " + p.price(), p.price() >= 2.0 && p.price() <= 50.0);
		}
	}

	@Test
	public void filterByPrice_greaterThan() {
		var products = repo().fetch(new ProductCriteria().price().gt(100.0));

		assertEquals(1, products.size());
		assertEquals(DBReset.COFFEE_MAKER_ID, products.get(0).id());
	}

	@Test
	public void filterByDescription_text() {
		var products = repo().fetch(new ProductCriteria().description().containing("Teflon"));

		assertEquals(1, products.size());
		assertEquals(DBReset.THREAD_SEAL_TAPE_ID, products.get(0).id());
	}

	@Test
	public void filterByTwoDifferentFields_combinesWithAnd() {
		// Across fields the join is always AND, even when each of them is disjunctive inside.
		var criteria = new ProductCriteria();
		criteria.name().containing("a");
		criteria.price().lt(50.0);

		var products = repo().fetch(criteria);
		for (var p : products) {
			assertTrue(p.name().toLowerCase().contains("a"));
			assertTrue(p.price() < 50.0);
		}
	}

	// :: Provisioned orderings — each one is an effect, not a field with a direction

	private List<String> namesOrderedBy(ProductCriteria.OrderBy order) {
		return repo().fetch(new ProductCriteria()
				.withOrderBy(order)).stream().map(Product::name).toList();
	}

	@Test
	public void orderBy_nameAToZ() {
		var names = namesOrderedBy(ProductCriteria.OrderBy.NAME_A_TO_Z);

		assertEquals(names.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), names);
		// And it does not match the insertion order — otherwise the test would pass even without sorting.
		assertNotEquals(namesOrderedBy(ProductCriteria.OrderBy.OLDEST_FIRST), names);
	}

	@Test
	public void orderBy_cheapestFirst() {
		var prices = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.CHEAPEST_FIRST))
				.stream().map(Product::price).toList();

		assertEquals(prices.stream().sorted().toList(), prices);
	}

	@Test
	public void orderBy_mostExpensiveFirst_isTheReverse() {
		var mostExpensive = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.MOST_EXPENSIVE_FIRST))
				.stream().map(Product::price).toList();
		var cheapest = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.CHEAPEST_FIRST))
				.stream().map(Product::price).toList();

		assertEquals(cheapest.reversed(), mostExpensive);
	}

	@Test
	public void orderBy_oldestAndNewest_areOpposites() {
		var oldest = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.OLDEST_FIRST)).stream().map(Product::id).toList();
		var newest = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.NEWEST_FIRST)).stream().map(Product::id).toList();

		assertEquals(oldest.reversed(), newest);
	}
}
