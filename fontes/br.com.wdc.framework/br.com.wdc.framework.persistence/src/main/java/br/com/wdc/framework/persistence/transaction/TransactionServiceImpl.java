package br.com.wdc.framework.persistence.transaction;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.sql.DataSource;

import br.com.wdc.framework.domain.transaction.TransactionContext;
import br.com.wdc.framework.domain.transaction.TransactionNotAllowedException;
import br.com.wdc.framework.domain.transaction.TransactionRequiredException;
import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;

/**
 * {@link TransactionService} implementation in the EJB CMT style, dual-mode (JTA/JDBC) and reentrant.
 *
 * <p>
 * Built on the {@link TransactionScope} frame (named {@code jdbcTx} in the helpers):
 * REQUIRED propagation joins the active transaction sharing the same connection; REQUIRES_NEW/NOT_SUPPORTED suspend the
 * current one and resume it at the end (in JDBC mode, opening a second connection).
 * </p>
 *
 * <p>
 * {@code work} semantics: COMMIT on normal return (only the owner commits), ROLLBACK on any exception (rethrown intact)
 * or if {@link TransactionContext#setRollbackOnly()} was called. Failures while finishing the transaction become
 * {@link TransactionSystemException}.
 * </p>
 *
 * <p>
 * <b>Single-thread:</b> the transaction is bound to the thread (ThreadLocal). Do not share the connection across
 * threads.
 * </p>
 */
public final class TransactionServiceImpl implements TransactionService {

    /** Handle given to {@code work}; reads the state of the ambient transaction. */
    private static final TransactionContext HANDLE = new AmbientContext();

    /** DataSource of this context (per module). Lazy resolution — the backend injects it in the module bootstrap. */
    private final Supplier<DataSource> dataSourceSupplier;

    public TransactionServiceImpl(Supplier<DataSource> dataSourceSupplier) {
        this.dataSourceSupplier = dataSourceSupplier;
    }

    // :: REQUIRED

    @Override
    public void required(Consumer<TransactionContext> work) {
        requiredCall(toFunction(work));
    }

    @Override
    public <T> T requiredCall(Function<TransactionContext, T> work) {
        return finish(beginRequired(), work);
    }

    // :: REQUIRES_NEW

    @Override
    public void requiresNew(Consumer<TransactionContext> work) {
        requiresNewCall(toFunction(work));
    }

    @Override
    public <T> T requiresNewCall(Function<TransactionContext, T> work) {
        var suspended = TransactionScope.suspend();
        try {
            return finish(beginNew(), work);
        } finally {
            TransactionScope.resume(suspended);
        }
    }

    // :: MANDATORY

    @Override
    public void mandatory(Consumer<TransactionContext> work) {
        mandatoryCall(toFunction(work));
    }

    @Override
    public <T> T mandatoryCall(Function<TransactionContext, T> work) {
        if (!txActive()) {
            throw new TransactionRequiredException("mandatory: no active transaction in scope");
        }
        return finish(beginRequired(), work);
    }

    // :: SUPPORTS

    @Override
    public void supports(Consumer<TransactionContext> work) {
        supportsCall(toFunction(work));
    }

    @Override
    public <T> T supportsCall(Function<TransactionContext, T> work) {
        if (txActive()) {
            return finish(beginRequired(), work);
        }
        return work.apply(HANDLE); // no transaction
    }

    // :: NOT_SUPPORTED

    @Override
    public void notSupported(Consumer<TransactionContext> work) {
        notSupportedCall(toFunction(work));
    }

    @Override
    public <T> T notSupportedCall(Function<TransactionContext, T> work) {
        var suspended = TransactionScope.suspend();
        try {
            return work.apply(HANDLE); // no transaction
        } finally {
            TransactionScope.resume(suspended);
        }
    }

    // :: NEVER

    @Override
    public void never(Consumer<TransactionContext> work) {
        neverCall(toFunction(work));
    }

    @Override
    public <T> T neverCall(Function<TransactionContext, T> work) {
        if (txActive()) {
            throw new TransactionNotAllowedException("never: there is an active transaction in scope");
        }
        return work.apply(HANDLE);
    }

    // -------------------------------------------------------------------------
    // :: Core (jdbcTx frame)

    /**
     * Runs {@code work} in the transaction frame: commits on success (only the owner commits; a participant is a
     * no-op), rolls back on exception (rethrown) and closes the frame.
     */
    private static <T> T finish(TransactionScope jdbcTx,
            Function<TransactionContext, T> work) {
        T result;
        try {
            result = work.apply(HANDLE);
        } catch (RuntimeException e) {
            jdbcTx.setRollbackOnly();
            closeSwallowing(jdbcTx, e);
            throw e;
        }
        try {
            jdbcTx.close(); // owner: commit (if not marked) or rollback; participant: no-op
        } catch (Exception e) {
            throw new TransactionSystemException("Failed to finish the transaction", e);
        }
        return result;
    }

    private TransactionScope beginRequired() {
        try {
            return TransactionScope.begin(dataSource());
        } catch (Exception e) {
            throw new TransactionSystemException("Failed to open/join the transaction", e);
        }
    }

    private TransactionScope beginNew() {
        try {
            return TransactionScope.beginNew(dataSource());
        } catch (Exception e) {
            throw new TransactionSystemException("Failed to open a new transaction", e);
        }
    }

    private static boolean txActive() {
        return TransactionScope.isActive();
    }

    /** Closes the frame on the error path without masking the original exception (adds it as suppressed). */
    private static void closeSwallowing(TransactionScope jdbcTx, Throwable original) {
        try {
            jdbcTx.close();
        } catch (Exception suppressed) {
            original.addSuppressed(suppressed);
        }
    }

    private DataSource dataSource() {
        var ds = dataSourceSupplier != null ? dataSourceSupplier.get() : null;
        if (ds == null) {
            throw new TransactionSystemException("DataSource not initialized for this TransactionService");
        }
        return ds;
    }

    private static <T> Function<TransactionContext, T> toFunction(Consumer<TransactionContext> work) {
        return tx -> {
            work.accept(tx);
            return null;
        };
    }

    // -------------------------------------------------------------------------
    // :: Ambient handle given to work

    private static final class AmbientContext implements TransactionContext {

        @Override
        public void setRollbackOnly() {
            var current = TransactionScope.current();
            if (current == null) {
                throw new IllegalStateException("setRollbackOnly: no active transaction in this scope");
            }
            current.setRollbackOnly();
        }

        @Override
        public boolean isRollbackOnly() {
            var current = TransactionScope.current();
            return current != null && current.isRollbackOnly();
        }

        @Override
        public boolean isActive() {
            return TransactionScope.isActive();
        }
    }
}
