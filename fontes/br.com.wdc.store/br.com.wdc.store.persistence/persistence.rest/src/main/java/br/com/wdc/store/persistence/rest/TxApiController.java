package br.com.wdc.store.persistence.rest;

import java.util.Map;

import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;
import br.com.wdc.store.persistence.rest.security.SecurityEnforcer;
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
     * Opaque identifier of the client context that demarcates transactions — a browser tab, an app instance — generated
     * by the client and sent on every call. With the user, it is the owner of the remote transactions: two tabs of one
     * user each own theirs.
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
     * Opaque transaction owner key for the current requester: the client context ({@value #CLIENT_HEADER}) that
     * demarcates, within the user's namespace.
     * <ul>
     * <li>authenticated → {@code "user:<id>:<client>"}, or {@code "user:<id>"} from a client that sends no
     * {@value #CLIENT_HEADER} — another user's transaction stays out of reach even with its ids;</li>
     * <li>no user (security disabled) → {@code "anon:<client>"}, or {@code null} without the header (each write its
     * own transaction).</li>
     * </ul>
     * The key correlates requests with a transaction, and keeps one client from finishing another's; what each
     * request may do is the security's to decide, per request. Used by begin/commit/rollback/status and by the
     * {@code transactional} decorator (resume, and the guard against a write outside the client's open transaction).
     */
    static String currentOwnerKey(Context ctx) {
        var clientId = ctx.header(CLIENT_HEADER);
        var client = clientId != null && !clientId.isBlank() ? clientId.trim() : null;
        var sc = SecurityContext.CURRENT.get();
        if (sc != null) {
            return client != null ? "user:" + sc.userId() + ":" + client : "user:" + sc.userId();
        }
        return client != null ? "anon:" + client : null;
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
