package br.com.wdc.shopping.persistence.rest.security;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * Security check utility for the REST endpoints.
 * <p>
 * Checks that there is a {@link SecurityContext} on the current thread and that it holds the requested permission.
 * <p>
 * If the {@link AuthenticationService} is not configured (test/local mode), the check is skipped and it returns
 * {@code null}.
 */
public final class SecurityEnforcer {

    private SecurityEnforcer() {
    }

    /**
     * Requires a SecurityContext that holds the requested permission.
     * <p>
     * If the {@link AuthenticationService} is not configured, returns {@code null} (no-security mode — tests or local
     * access).
     *
     * @return the current SecurityContext, or {@code null} if security is not active
     * @throws AccessDeniedException if security is active and the user is not authenticated or lacks the permission
     */
    /**
     * Requires only an <b>authenticated</b> user (no specific permission). Returns {@code null} if security is not
     * active.
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

    public static SecurityContext require(String entity, String operation) {
        if (AuthenticationService.BEAN.get() == null) {
            return null;
        }
        var sc = SecurityContext.CURRENT.get();
        if (sc == null) {
            throw new AccessDeniedException("Authentication required");
        }
        if (!sc.hasPermission(entity, operation)) {
            throw new AccessDeniedException("Requires " + entity + ":" + operation);
        }
        return sc;
    }

}
