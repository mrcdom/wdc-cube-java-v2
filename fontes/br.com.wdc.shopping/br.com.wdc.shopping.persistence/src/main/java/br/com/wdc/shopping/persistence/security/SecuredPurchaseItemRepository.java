package br.com.wdc.shopping.persistence.security;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.PurchaseItemCriteria;
import br.com.wdc.shopping.domain.model.PurchaseItem;
import br.com.wdc.shopping.domain.repositories.PurchaseItemRepository;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Decorator seguro para {@link PurchaseItemRepository}.
 * <p>
 * Non-admin: restringe consultas ao userId da sessão.
 */
public final class SecuredPurchaseItemRepository implements PurchaseItemRepository {

	private static final String ENTITY = "purchase-item";

	private final PurchaseItemRepository delegate;

	public SecuredPurchaseItemRepository(PurchaseItemRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean insert(PurchaseItem purchaseItem) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.insert(purchaseItem);
	}

	@Override
	public boolean insertOrUpdate(PurchaseItem purchaseItem) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.insertOrUpdate(purchaseItem);
	}

	@Override
	public boolean update(PurchaseItem newPurchaseItem, PurchaseItem oldPurchaseItem) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.update(newPurchaseItem, oldPurchaseItem);
	}

	@Override
	public int delete(PurchaseItemCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "delete");
		enforceUserScope(sc, criteria);
		return delegate.delete(criteria);
	}

	@Override
	public int count(PurchaseItemCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "read");
		enforceUserScope(sc, criteria);
		return delegate.count(criteria);
	}

	@Override
	public List<PurchaseItem> fetch(PurchaseItemCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "read");
		enforceUserScope(sc, criteria);
		return delegate.fetch(criteria);
	}

	@Override
	public PurchaseItem fetchById(Long purchaseItemId, PurchaseItem projection) {
		SecurityEnforcer.require(ENTITY, "read");
		return delegate.fetchById(purchaseItemId, projection);
	}

	// :: Scope enforcement

	private static void enforceUserScope(SecurityContext sc, PurchaseItemCriteria criteria) {
		if (!sc.hasDataAll()) {
			criteria.withUserId(sc.userId());
		}
	}
}
