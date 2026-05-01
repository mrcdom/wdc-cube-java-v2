package br.com.wdc.shopping.persistence.security;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.PurchaseCriteria;
import br.com.wdc.shopping.domain.model.Purchase;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.PurchaseRepository;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Decorator seguro para {@link PurchaseRepository}.
 * <p>
 * Non-admin: restringe todas as consultas/modificações ao userId da sessão.
 */
public final class SecuredPurchaseRepository implements PurchaseRepository {

	private static final String ENTITY = "purchase";

	private final PurchaseRepository delegate;

	public SecuredPurchaseRepository(PurchaseRepository delegate) {
		this.delegate = delegate;
	}

	@Override
	public boolean insert(Purchase purchase) {
		var sc = SecurityEnforcer.require(ENTITY, "write");
		enforceUserScope(sc, purchase);
		return delegate.insert(purchase);
	}

	@Override
	public boolean insertOrUpdate(Purchase purchase) {
		var sc = SecurityEnforcer.require(ENTITY, "write");
		enforceUserScope(sc, purchase);
		return delegate.insertOrUpdate(purchase);
	}

	@Override
	public boolean update(Purchase newPurchase, Purchase oldPurchase) {
		SecurityEnforcer.require(ENTITY, "write");
		return delegate.update(newPurchase, oldPurchase);
	}

	@Override
	public int delete(PurchaseCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "delete");
		enforceUserScope(sc, criteria);
		return delegate.delete(criteria);
	}

	@Override
	public int count(PurchaseCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "read");
		enforceUserScope(sc, criteria);
		return delegate.count(criteria);
	}

	@Override
	public List<Purchase> fetch(PurchaseCriteria criteria) {
		var sc = SecurityEnforcer.require(ENTITY, "read");
		enforceUserScope(sc, criteria);
		return delegate.fetch(criteria);
	}

	@Override
	public Purchase fetchById(Long purchaseId, Purchase projection) {
		var sc = SecurityEnforcer.require(ENTITY, "read");
		var result = delegate.fetchById(purchaseId, projection);
		if (result != null && !sc.hasDataAll()
				&& result.user != null && !sc.userId().equals(result.user.id)) {
			return null;
		}
		return result;
	}

	// :: Scope enforcement

	private static void enforceUserScope(SecurityContext sc, PurchaseCriteria criteria) {
		if (!sc.hasDataAll()) {
			criteria.withUserId(sc.userId());
		}
	}

	private static void enforceUserScope(SecurityContext sc, Purchase purchase) {
		if (!sc.hasDataAll()) {
			if (purchase.user == null) {
				purchase.user = new User();
			}
			purchase.user.id = sc.userId();
		}
	}
}
