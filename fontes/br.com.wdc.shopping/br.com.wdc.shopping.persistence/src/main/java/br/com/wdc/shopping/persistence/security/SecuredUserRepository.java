package br.com.wdc.shopping.persistence.security;

import java.util.List;

import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.Page;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Decorator seguro para {@link UserRepository}.
 * <p>
 * Verifica permissões, restringe escopo ao próprio usuário (non-admin), e garante que o campo password nunca seja
 * retornado.
 */
public final class SecuredUserRepository implements UserRepository {

    private static final String ENTITY = "user";

    private final UserRepository delegate;

    public SecuredUserRepository(UserRepository delegate) {
        this.delegate = delegate;
    }

    @Override
    public User newProjection() {
        return delegate.newProjection();
    }

    @Override
    public boolean insert(User user) {
        var sc = SecurityEnforcer.require(ENTITY, "write");
        enforceUserScope(sc, user);
        return delegate.insert(user);
    }

    @Override
    public boolean update(User newUser, User oldUser) {
        var sc = SecurityEnforcer.require(ENTITY, "write");
        enforceUserScope(sc, newUser);
        return delegate.update(newUser, oldUser);
    }

    @Override
    public boolean insertOrUpdate(User newUser, User oldUser) {
        var sc = SecurityEnforcer.require(ENTITY, "write");
        enforceUserScope(sc, newUser);
        enforceUserScope(sc, oldUser);
        return delegate.insertOrUpdate(newUser, oldUser);
    }

    @Override
    public int delete(UserCriteria criteria) {
        var sc = SecurityEnforcer.require(ENTITY, "delete");
        enforceUserScope(sc, criteria);
        return delegate.delete(criteria);
    }

    @Override
    public int count(UserCriteria criteria) {
        var sc = SecurityEnforcer.require(ENTITY, "read");
        enforceUserScope(sc, criteria);
        return delegate.count(criteria);
    }

    @Override
    public List<User> fetch(UserCriteria criteria, int offset, int limit) {
        var sc = SecurityEnforcer.require(ENTITY, "read");
        enforceUserScope(sc, criteria);
        sanitizeProjection(criteria);
        var results = delegate.fetch(criteria, offset, limit);
        results.forEach(u -> u.password = null);
        return results;
    }

    @Override
    public Page<User> fetchPage(UserCriteria criteria, int page, int pageSize) {
        SecurityEnforcer.require(ENTITY, "read");
        return delegate.fetchPage(criteria, page, pageSize);
    }

    @Override
    public User fetchById(Long userId, User projection) {
        var sc = SecurityEnforcer.require(ENTITY, "read");
        if (!sc.hasDataAll() && !userId.equals(sc.userId())) {
            return null;
        }
        stripPassword(projection);
        var result = delegate.fetchById(userId, projection);
        if (result != null) {
            result.password = null;
        }
        return result;
    }

    // :: Security helpers

    private static void enforceUserScope(SecurityContext sc, UserCriteria criteria) {
        if (!sc.hasDataAll()) {
            criteria.withUserId(sc.userId());
        }
    }

    private static void enforceUserScope(SecurityContext sc, User user) {
        if (user == null) {
            return;
        }
        if (!sc.hasDataAll() && user.id != null && !user.id.equals(sc.userId())) {
            throw new br.com.wdc.shopping.domain.exception.AccessDeniedException("Cannot modify other user's data");
        }
    }

    private static void sanitizeProjection(UserCriteria criteria) {
        if (criteria.projection() != null) {
            criteria.projection().password = null;
        }
    }

    private static void stripPassword(User projection) {
        if (projection != null) {
            projection.password = null;
        }
    }
}
