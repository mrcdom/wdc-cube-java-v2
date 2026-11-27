package br.com.wdc.store.persistence.rest;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.transaction.UnknownTransactionException;
import br.com.wdc.store.domain.StoreTransactions;
import br.com.wdc.store.persistence.rest.doc.RepositoryApiDocs;
import br.com.wdc.store.persistence.rest.security.SecurityFilter;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Handler;

/**
 * Registers all REST endpoints of the repository API in Javalin.
 * <p>
 * If the {@link AuthenticationService} is initialized (via {@code StoreRepositoryBootstrap.initializeSecurity}),
 * it automatically registers the security filter and the authentication endpoints.
 */
public final class RepositoryApiRoutes {

    private RepositoryApiRoutes() {
        // NOOP
    }

    /**
     * Configures the REST routes with a context prefix.
     * <p>
     * Allows registering the API under {@code /<context>/api/repo/...} so that SPAs served under a context reach the API
     * without CORS.
     */
    public static void configure(JavalinConfig config, String prefix) {
        ApiResponseHeaders.configure(config, prefix);

        var authService = AuthenticationService.BEAN.get();
        if (authService != null) {
            // Public authentication endpoints
            AuthApiController.configure(config, prefix);

            // Security filter for protected endpoints
            var securityFilter = new SecurityFilter();
            config.routes.before(prefix + "/api/repo/*", securityFilter::handle);
            config.routes.after(prefix + "/api/repo/*", ctx -> SecurityContext.CURRENT.remove());
            // Remote transaction endpoints also require authentication
            config.routes.before(prefix + "/api/tx/*", securityFilter::handle);
            config.routes.after(prefix + "/api/tx/*", ctx -> SecurityContext.CURRENT.remove());
        }

        // Exception handler for AccessDeniedException
        config.routes.exception(AccessDeniedException.class, (e, ctx) -> {
            ctx.status(403);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        });

        // Uninterpretable request (e.g. an ordering name this server does not know) → 400, with the reason in the body
        config.routes.exception(InvalidRequestException.class, (e, ctx) -> {
            ctx.status(400);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        });

        // Transactional state conflict (e.g. a write without X-Tx-Id while a tx is open) → 409, not 403
        config.routes.exception(TransactionConflictException.class, (e, ctx) -> {
            ctx.status(409);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        });

        // A remote transaction this instance does not know (ended, reaped, or held by another instance) → 410
        config.routes.exception(UnknownTransactionException.class, (e, ctx) -> {
            ctx.status(410);
            ctx.json(java.util.Map.of("error", e.getMessage()));
        });

        // Must wait before trying again (the cap on open remote transactions, the sign-in limits) → 429, with the wait
        // in Retry-After and in the body when known
        config.routes.exception(TooManyRequestsException.class, (e, ctx) -> {
            ctx.status(429);
            var seconds = e.retryAfterSeconds();
            if (seconds == null) {
                ctx.json(java.util.Map.of("error", e.getMessage()));
            } else {
                ctx.header("Retry-After", Long.toString(seconds));
                ctx.json(java.util.Map.of("error", e.getMessage(), "retryAfter", seconds));
            }
        });

        // Entity controllers
        UserApiController.configure(config, prefix);
        ProductApiController.configure(config, prefix);
        PurchaseApiController.configure(config, prefix);
        PurchaseItemApiController.configure(config, prefix);
        TxApiController.configure(config, prefix);

        // OpenAPI document — served at GET {prefix}/openapi.json
        config.routes.get(prefix + "/openapi.json", ctx -> {
            ctx.contentType("application/json");
            ctx.result(RepositoryApiDocs.toJson(prefix));
        });
    }

    /**
     * Wraps a <b>write</b> handler in a transaction ({@code TransactionService.required}), so that multiple statements
     * (e.g. purchase + items) commit/roll back atomically. On normal return it commits; on exception it rolls back and
     * rethrows the <b>original</b> exception (type preserved), so that Javalin's exception mappers keep working. If no
     * {@link TransactionService} is registered, it runs directly.
     */
    static Handler transactional(Handler delegate) {
        return ctx -> {
            var txId = ctx.header(TxApiController.TX_HEADER);
            var coordinator = RemoteTransactions.COORDINATOR.get();

            // Client-driven remote transaction: joins the txId's tx (resume/suspend) without committing —
            // the boundary (commit/rollback) belongs to the client, via TxApiController.
            if (txId != null && !txId.isBlank() && coordinator != null) {
                coordinator.resume(txId, TxApiController.currentOwnerKey(ctx));
                try {
                    delegate.handle(ctx);
                } finally {
                    coordinator.suspend(txId);
                }
                return;
            }

            // No txId, but the requester has an open remote transaction: the X-Tx-Id header got lost on the way.
            // Reject instead of autocommitting this write outside the transaction (which would leave an orphan record,
            // breaking the block's atomicity). Server-side defense of the client's propagation.
            if (coordinator != null) {
                var owner = TxApiController.currentOwnerKey(ctx);
                if (owner != null && coordinator.hasOpenTransactionForOwner(owner)) {
                    throw new TransactionConflictException("Write without " + TxApiController.TX_HEADER
                            + " while the requester has an open remote transaction — aborted to preserve atomicity");
                }
            }

            // No txId: per-request transaction (isolated atomic write).
            var tx = StoreTransactions.BEAN.get();
            if (tx == null) {
                delegate.handle(ctx);
                return;
            }
            tx.required(t -> {
                try {
                    delegate.handle(ctx);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        };
    }
}
