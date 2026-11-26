package br.com.wdc.store.persistence.rest.security;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;
import io.javalin.http.Context;

/**
 * HTTP security filter for the {@code /api/repo/**} endpoints.
 * <p>
 * Extracts the Bearer token, delegates validation to the {@link AuthenticationService}, and populates the
 * {@link SecurityContextHolder} for the current request.
 */
public final class SecurityFilter {

    private static final Log LOG = Log.getLogger(SecurityFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Handler to be registered as {@code before("/api/repo/*")}.
     * <p>
     * Reading a product image is public (the catalog's pictures); changing one is not, and goes through the policy.
     */
    public void handle(Context ctx) {
        // Skip CORS preflight requests (OPTIONS) — handled by CORS plugin
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            return;
        }

        if (isPublicRoute(ctx)) {
            return;
        }

        var authHeader = ctx.header("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            ctx.status(401).contentType("application/json")
                    .result("{\"error\":\"Missing or invalid Authorization header\"}");
            ctx.skipRemainingHandlers();
            return;
        }

        var token = authHeader.substring(BEARER_PREFIX.length());
        
        var authService = AuthenticationService.BEAN.get();
        var securityContext = authService.resolveToken(token);
        if (securityContext == null) {
            ctx.status(401).contentType("application/json").result("{\"error\":\"Invalid or expired token\"}");
            ctx.skipRemainingHandlers();
            return;
        }

        SecurityContext.CURRENT.set(securityContext);
        LOG.debug("Authenticated request: user={} path={}", securityContext.userName(), ctx.path());
    }

    private static boolean isPublicRoute(Context ctx) {
        return "GET".equalsIgnoreCase(ctx.method().name()) && ctx.path().endsWith("/image");
    }
}
