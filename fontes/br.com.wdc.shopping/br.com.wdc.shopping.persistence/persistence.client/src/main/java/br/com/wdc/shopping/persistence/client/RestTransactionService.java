package br.com.wdc.shopping.persistence.client;

import java.util.function.Consumer;
import java.util.function.Function;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.transaction.TransactionContext;
import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;

/**
 * Client-side (REST) implementation of {@link TransactionService} — a client-driven remote coordinator.
 *
 * <p>
 * {@code required} opens a remote transaction (<b>begin</b> → {@code txId}), propagates the {@code txId} (via
 * {@code HttpTransport.setTransactionIdSupplier} → {@code X-Tx-Id} header) on the block's repository calls — which
 * join the same physical transaction on the server — and finishes with <b>commit</b> (normal return, unless
 * {@code setRollbackOnly}) or <b>rollback</b> (exception). That way multiple REST writes become atomic.
 * </p>
 *
 * <p>
 * The current {@code txId} is bound to the thread ({@link ThreadLocal}); nesting {@code required} reuses the active
 * transaction (participates). Single-threaded, like the server coordinator.
 * </p>
 */
public final class RestTransactionService implements TransactionService {

    /**
     * Active remote transaction, bound to the thread. Isolation between concurrent flows relies on an
     * <b>invariant</b>: each independent user action runs on its own thread. In the backend and in JVM clients this is
     * trivial (real threads). In the browser (TeaVM, single-threaded with an event loop) it holds too <b>because</b> (1)
     * each action is dispatched on its own green thread (e.g. {@code safeAction} → {@code new Thread().start()}) and
     * (2) TeaVM's {@code ThreadLocal} is per-thread (stored by {@code currentThread().key}). So if a {@code required}
     * block suspends on an XHR and another action runs on the event loop, that action reads its own {@code CURRENT}
     * (empty) and does <b>not</b> inherit the suspended {@code txId}. <b>Careful when refactoring:</b> running actions
     * inline on the main thread or reusing threads from a pool would break this isolation.
     */
    private static final ThreadLocal<TxState> CURRENT = new ThreadLocal<>();
    private static final TransactionContext NO_TX = new NoTxContext();

    private final HttpTransport transport;

    public RestTransactionService(HttpTransport transport) {
        this.transport = transport;
        // The transport sends X-Tx-Id whenever there is an active remote transaction on this thread.
        transport.setTransactionIdSupplier(() -> {
            var state = CURRENT.get();
            return state != null ? state.txId : null;
        });
    }

    // :: REQUIRED

    @Override
    public void required(Consumer<TransactionContext> work) {
        requiredCall(toFunction(work));
    }

    @Override
    public <T> T requiredCall(Function<TransactionContext, T> work) {
        var existing = CURRENT.get();
        if (existing != null) {
            return work.apply(existing); // already in a remote transaction: participate
        }
        return runInNewTx(work);
    }

    // :: REQUIRES_NEW

    @Override
    public void requiresNew(Consumer<TransactionContext> work) {
        requiresNewCall(toFunction(work));
    }

    @Override
    public <T> T requiresNewCall(Function<TransactionContext, T> work) {
        var outer = CURRENT.get();
        if (outer != null) {
            CURRENT.remove(); // suspend: inner calls do not carry the outer txId
        }
        try {
            return runInNewTx(work);
        } finally {
            if (outer != null) {
                CURRENT.set(outer);
            }
        }
    }

    // :: MANDATORY

    @Override
    public void mandatory(Consumer<TransactionContext> work) {
        mandatoryCall(toFunction(work));
    }

    @Override
    public <T> T mandatoryCall(Function<TransactionContext, T> work) {
        var state = CURRENT.get();
        if (state == null) {
            throw new TransactionRequiredException("mandatory: no active remote transaction");
        }
        return work.apply(state);
    }

    // :: SUPPORTS

    @Override
    public void supports(Consumer<TransactionContext> work) {
        supportsCall(toFunction(work));
    }

    @Override
    public <T> T supportsCall(Function<TransactionContext, T> work) {
        var state = CURRENT.get();
        return work.apply(state != null ? state : NO_TX);
    }

    // :: NOT_SUPPORTED

    @Override
    public void notSupported(Consumer<TransactionContext> work) {
        notSupportedCall(toFunction(work));
    }

    @Override
    public <T> T notSupportedCall(Function<TransactionContext, T> work) {
        var outer = CURRENT.get();
        if (outer != null) {
            CURRENT.remove(); // suspend: inner calls carry no txId
        }
        try {
            return work.apply(NO_TX);
        } finally {
            if (outer != null) {
                CURRENT.set(outer);
            }
        }
    }

    // :: NEVER

    @Override
    public void never(Consumer<TransactionContext> work) {
        neverCall(toFunction(work));
    }

    @Override
    public <T> T neverCall(Function<TransactionContext, T> work) {
        if (CURRENT.get() != null) {
            throw new TransactionNotAllowedException("never: there is an active remote transaction");
        }
        return work.apply(NO_TX);
    }

    // -------------------------------------------------------------------------

    private <T> T runInNewTx(Function<TransactionContext, T> work) {
        var state = new TxState(remoteBegin());
        CURRENT.set(state);
        try {
            T result = work.apply(state);
            if (state.rollbackOnly) {
                remoteRollback();
            } else {
                remoteCommit();
            }
            return result;
        } catch (RuntimeException e) {
            remoteRollbackQuietly();
            throw e;
        } finally {
            CURRENT.remove();
        }
    }

    private String remoteBegin() {
        var response = transport.postJson("/api/tx/begin", "{}");
        var reader = new JsonStreamReader(response);
        reader.beginObject();
        var txId = (String) null;
        while (reader.hasNext()) {
            if ("txId".equals(reader.nextName())) {
                txId = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        if (txId == null || txId.isBlank()) {
            throw new TransactionSystemException("begin response without txId");
        }
        return txId;
    }

    /** The txId goes in the X-Tx-Id header (transport), since CURRENT is still set during commit/rollback. */
    private void remoteCommit() {
        transport.postJson("/api/tx/commit", "{}");
    }

    private void remoteRollback() {
        transport.postJson("/api/tx/rollback", "{}");
    }

    private void remoteRollbackQuietly() {
        try {
            remoteRollback();
        } catch (RuntimeException ignore) {
            // best effort: the work's original exception is the one that matters
        }
    }

    private static <T> Function<TransactionContext, T> toFunction(Consumer<TransactionContext> work) {
        return tx -> {
            work.accept(tx);
            return null;
        };
    }

    /** Handle of an active remote transaction. */
    private static final class TxState implements TransactionContext {
        final String txId;
        boolean rollbackOnly;

        TxState(String txId) {
            this.txId = txId;
        }

        @Override
        public void setRollbackOnly() {
            this.rollbackOnly = true;
        }

        @Override
        public boolean isRollbackOnly() {
            return rollbackOnly;
        }

        @Override
        public boolean isActive() {
            return true;
        }
    }

    /** Handle passed when there is no transaction (supports/notSupported/never without an active tx). */
    private static final class NoTxContext implements TransactionContext {
        @Override
        public void setRollbackOnly() {
            throw new IllegalStateException("setRollbackOnly: no active remote transaction in this scope");
        }

        @Override
        public boolean isRollbackOnly() {
            return false;
        }

        @Override
        public boolean isActive() {
            return false;
        }
    }
}
