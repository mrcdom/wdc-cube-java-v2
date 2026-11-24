package br.com.wdc.store.persistence.rest.security;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * The requester of a REST call.
 * <p>
 * What the requester may do with the data is decided by the entity's data policy ({@code XxxDataPolicy}), which takes
 * the context returned here. If the {@link AuthenticationService} is not configured (local or test mode), there is no
 * requester and it returns {@code null} — the policies then let everything through.
 */
public final class SecurityEnforcer {

    private SecurityEnforcer() {
    }

    /**
     * Requires an <b>authenticated</b> requester. Returns {@code null} if security is not active.
     *
     * @throws AccessDeniedException if security is active and there is no authenticated user
     */
    public static SecurityContext requireAuthenticated() {
        if (AuthenticationService.BEAN.get() == null) {
            return null;
        }
        var sc = SecurityContext.CURRENT.get();
        if (sc == null) {
            throw new AccessDeniedException("Authentication required");
        }
        return sc;
    }
}
