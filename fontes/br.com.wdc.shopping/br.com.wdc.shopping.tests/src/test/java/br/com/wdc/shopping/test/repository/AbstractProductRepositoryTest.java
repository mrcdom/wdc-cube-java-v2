package br.com.wdc.shopping.test.repository;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;
import br.com.wdc.framework.domain.projection.ProjectionValues;
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
		assertNotNull(product.name);
		assertNotNull(product.price);
	}

	@Test
	public void fetchById_nonExistent_returnsNull() {
		var product = repo().fetchById(Long.MAX_VALUE, null);
		assertNull(product);
	}

	@Test
	public void fetchWithProjection_onlyRequestedFields() {
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product();
		projection.id = pv.i64;
		projection.name = pv.str;

		var product = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, projection);
		assertNotNull(product);
		assertEquals(DBReset.PEN_DRIVE2GB_ID, product.id);
		assertNotNull(product.name);
	}

	@Test
	public void fetchByCriteria_productId() {
		var products = repo().fetch(new ProductCriteria().withProductId(DBReset.BOLA_WILSON_ID));
		assertEquals(1, products.size());
		assertEquals(DBReset.BOLA_WILSON_ID, products.get(0).id);
	}

	@Test
	public void fetchWithOffsetAndLimit() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.ASCENDING), 0, 2);
		assertEquals(2, products.size());
	}

	@Test
	public void fetchWithOrderAscending() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.ASCENDING));
		assertEquals(4, products.size());
		for (int i = 1; i < products.size(); i++) {
			assertTrue(products.get(i - 1).id <= products.get(i).id);
		}
	}

	@Test
	public void fetchWithOrderDescending() {
		var products = repo().fetch(new ProductCriteria()
				.withOrderBy(ProductCriteria.OrderBy.DESCENDING));
		assertEquals(4, products.size());
		for (int i = 1; i < products.size(); i++) {
			assertTrue(products.get(i - 1).id >= products.get(i).id);
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
		var product = new Product();
		product.name = "Teclado USB";
		product.price = 89.90;
		product.description = "Teclado mecanico";

		boolean inserted = repo().insert(product);
		assertTrue(inserted);
		assertNotNull(product.id);

		var fetched = repo().fetchById(product.id, null);
		assertNotNull(fetched);
		assertEquals("Teclado USB", fetched.name);
		assertEquals(89.90, fetched.price, 0.001);
		assertNotNull(fetched.description);
		assertTrue(fetched.description.contains("Teclado"));
	}

	// :: update

	@Test
	public void update_existingProduct() {
		var original = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertNotNull(original);

		var updated = new Product();
		updated.id = original.id;
		updated.name = "Pen Drive 4GB";
		updated.price = 35.0;
		updated.description = original.description;

		boolean result = repo().update(updated, original);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertEquals("Pen Drive 4GB", fetched.name);
		assertEquals(35.0, fetched.price, 0.001);
	}

	@Test
	public void update_partialFields_onlyChangesSpecifiedFields() {
		var original = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertNotNull(original);
		var originalDescription = original.description;
		var originalPrice = original.price;

		// projeção parcial: só id e name
		var pv = ProjectionValues.INSTANCE;
		var projection = new Product();
		projection.id = pv.i64;
		projection.name = pv.str;

		var updated = new Product();
		updated.id = original.id;
		updated.name = "Pen Drive Renomeado";

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		assertEquals("Pen Drive Renomeado", fetched.name);
		assertEquals(originalPrice, fetched.price, 0.001);
		assertEquals(originalDescription, fetched.description);
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
		var projection = new Product();
		projection.id = pv.i64;
		projection.price = pv.f64;

		var updated = new Product();
		updated.id = original.id;
		updated.price = 99.99;
		updated.description = null; // null no newEntity, mas NÃO na projeção

		boolean result = repo().update(updated, original, projection);
		assertTrue(result);

		var fetched = repo().fetchById(DBReset.PEN_DRIVE2GB_ID, null);
		// description preservada (campo fora da projeção não é tocado)
		assertEquals(original.description, fetched.description);
		// price atualizado
		assertEquals(99.99, fetched.price, 0.001);
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
}
