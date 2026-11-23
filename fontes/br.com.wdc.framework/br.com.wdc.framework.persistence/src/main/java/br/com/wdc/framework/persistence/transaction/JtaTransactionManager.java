package br.com.wdc.framework.persistence.transaction;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.transaction.TransactionManager;

/**
 * Static holder of the JTA {@link TransactionManager} used by the application.
 *
 * <p>
 * Initialized by the application's transaction bootstrap (e.g. {@code JtaSupport}, in the backend module) during
 * startup. Accessed by {@link TransactionScope} to demarcate JTA transactions.
 * </p>
 */
public final class JtaTransactionManager {

    public static final AtomicReference<TransactionManager> BEAN = new AtomicReference<>();

    private JtaTransactionManager() {
        // NOOP
    }

    /**
     * Returns the configured {@link TransactionManager}.
     *
     * @throws IllegalStateException if the bootstrap has not run yet
     */
    public static TransactionManager get() {
        var tm = BEAN.get();
        if (tm == null) {
            throw new IllegalStateException(
                    "JtaTransactionManager has not been initialized — check the JTA bootstrap (JtaSupport.init)");
        }
        return tm;
    }
}
