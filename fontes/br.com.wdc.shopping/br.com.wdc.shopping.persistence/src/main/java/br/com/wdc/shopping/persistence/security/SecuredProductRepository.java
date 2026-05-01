package br.com.wdc.shopping.persistence.security;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.ProductCriteria;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.repositories.ProductRepository;

/**
 * Decorator seguro para {@link ProductRepository}.
 * <p>
 * Produtos são catálogo compartilhado — sem restrição de escopo por usuário.
 * Verifica apenas permissões de entidade.
 */
public final class SecuredProductRepository implements ProductRepository {

	private static final String ENTITY = "product";

	private final ProductRepository delegate;

	public SecuredProductRepository(ProductRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean insert(Product product) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.insert(product);
	}

	@Override
	public boolean update(Product newProduct, Product oldProduct) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.update(newProduct, oldProduct);
	}

	@Override
	public boolean insertOrUpdate(Product product) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.insertOrUpdate(product);
	}

	@Override
	public int delete(ProductCriteria criteria) {
		SecurityEnforcer.require(ENTITY, "delete");
		return delegate.delete(criteria);
	}

	@Override
	public int count(ProductCriteria criteria) {
		SecurityEnforcer.require(ENTITY, "read");
		return delegate.count(criteria);
	}

	@Override
	public List<Product> fetch(ProductCriteria criteria) {
		SecurityEnforcer.require(ENTITY, "read");
		return delegate.fetch(criteria);
	}

	@Override
	public Product fetchById(Long productId, Product projection) {
		SecurityEnforcer.require(ENTITY, "read");
		return delegate.fetchById(productId, projection);
	}

	@Override
	public byte[] fetchImage(Long productId) {
		// Imagens de produto são recurso público (catálogo) — sem verificação de permissão
		return delegate.fetchImage(productId);
	}

	@Override
	public boolean updateImage(Long productId, byte[] image) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.updateImage(productId, image);
	}
}
