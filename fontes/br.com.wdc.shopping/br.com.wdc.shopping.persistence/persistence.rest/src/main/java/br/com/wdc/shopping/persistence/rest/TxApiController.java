package br.com.wdc.shopping.persistence.rest;

import java.util.Map;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;
import br.com.wdc.shopping.persistence.rest.security.SecurityEnforcer;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Endpoints of the <b>client-driven remote transaction</b>: {@code begin}/{@code commit}/{@code rollback}.
 *
 * <p>
 * The client opens a transaction ({@code begin} → {@code txId}), sends the {@code txId} in the {@value #TX_HEADER} header
 * on subsequent write calls (which join the same physical transaction on the server) and finishes with
 * {@code commit} or {@code rollback}.
 * </p>
 *
 * <p>
 * <b>Security:</b> the endpoints require authentication (filter on {@code /api/tx/*}). Ownership of the transaction is
 * held by the {@link RemoteTransactionCoordinator} itself: the user's identity (id) is passed as {@code ownerKey} on
 * {@code begin} and revalidated by the coordinator on resume/commit/rollback — another user cannot use it. There is no
 * ownership state here (the whole lifecycle, incl. the reaper, belongs to the coordinator).
 * </p>
 */
public final class TxApiController {

    public static final String TX_HEADER = "X-Tx-Id";

    /**
     * Opaque identifier of the <b>client session</b> (generated per client app instance). Used as the owner of the
     * remote transaction when there is no authenticated user (security disabled) — so every transaction has a
     * distinguishable owner, even an anonymous one.
     */
    public static final String CLIENT_HEADER = "X-Client-Id";

    private TxApiController() {
        // NOOP
    }

    public static void configure(JavalinConfig config, String prefix) {
        config.routes.post(prefix + "/api/tx/begin", TxApiController::begin);
        config.routes.post(prefix + "/api/tx/commit", TxApiController::commit);
        config.routes.post(prefix + "/api/tx/rollback", TxApiController::rollback);
        config.routes.get(prefix + "/api/tx/status", TxApiController::status);
    }

    private static void begin(Context ctx) {
        SecurityEnforcer.requireAuthenticated();
        var txId = coordinator().begin(currentOwnerKey(ctx));
        ctx.json(Map.of("txId", txId));
    }

    private static void commit(Context ctx) {
        coordinator().commit(txIdHeader(ctx), currentOwnerKey(ctx));
        ctx.json(Map.of("status", "committed"));
    }

    private static void rollback(Context ctx) {
        coordinator().rollback(txIdHeader(ctx), currentOwnerKey(ctx));
        ctx.json(Map.of("status", "rolledback"));
    }

    /** Queries the state of a transaction (open/committed/rolledback/unknown) — disambiguates a lost response. */
    private static void status(Context ctx) {
        ctx.json(Map.of("status", coordinator().status(txIdHeader(ctx), currentOwnerKey(ctx))));
    }

    /**
     * Opaque transaction owner key for the current requester. In separate namespaces to keep an anonymous client from
     * impersonating (or colliding with) a real user:
     * <ul>
     * <li>authenticated → {@code "user:" + userId} (any {@value #CLIENT_HEADER} is <b>ignored</b>, not spoofable);</li>
     * <li>no user (security disabled) → {@code "anon:" + }{@value #CLIENT_HEADER}, or {@code null} if the header is
     * absent (legacy client — falls back to per-request transactions).</li>
     * </ul>
     * Used by begin/commit/rollback and by the {@code transactional} decorator (rebind/guard).
     */
    static String currentOwnerKey(Context ctx) {
        var sc = SecurityContext.CURRENT.get();
        if (sc != null) {
            return "user:" + sc.userId();
        }
        var clientId = ctx.header(CLIENT_HEADER);
        return (clientId != null && !clientId.isBlank()) ? "anon:" + clientId : null;
    }

    private static String txIdHeader(Context ctx) {
        var txId = ctx.header(TX_HEADER);
        if (txId == null || txId.isBlank()) {
            throw new AccessDeniedException("Missing " + TX_HEADER + " header");
        }
        return txId;
    }

    private static RemoteTransactionCoordinator coordinator() {
        var c = RemoteTransactions.COORDINATOR.get();
        if (c == null) {
            throw new IllegalStateException("Remote transaction coordinator not initialized");
        }
        return c;
    }
}
