package br.com.wdc.shopping.persistence.rest.security;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;
import io.javalin.http.Context;

/**
 * Filtro de segurança HTTP para endpoints {@code /api/repo/**}.
 * <p>
 * Extrai o Bearer token, delega validação ao {@link AuthenticationService}, e popula o {@link SecurityContextHolder}
 * para a requisição corrente.
 */
public final class SecurityFilter {

    private static final Log LOG = Log.getLogger(SecurityFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Handler a ser registrado como {@code before("/api/repo/*")}.
     * <p>
     * Rotas de imagem de produto são públicas (catálogo) e não exigem autenticação.
     */
    public void handle(Context ctx) {
        // Skip CORS preflight requests (OPTIONS) — handled by CORS plugin
        if ("OPTIONS".equalsIgnoreCase(ctx.method().name())) {
            return;
        }

        if (isPublicRoute(ctx.path())) {
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

    private static boolean isPublicRoute(String path) {
        return path.endsWith("/image");
    }
}
