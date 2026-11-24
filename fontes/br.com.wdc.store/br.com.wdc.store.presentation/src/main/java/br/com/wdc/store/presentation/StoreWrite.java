package br.com.wdc.store.presentation;

import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.store.domain.StoreTransactions;

/**
 * A write: {@link #run()} opens the transaction (or joins the caller's) and does the work in {@link #write()}, so no
 * write goes out of one.
 * <p>
 * Only the write verbs extend it ({@link StoreCreate}, {@link StoreSave}, {@link StoreUpdate}, {@link StoreDelete}):
 * the constructor is package-private. A call extends the verb, and implements the method named after it.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreWrite<R> extends StoreCall<R> {

    StoreWrite() {
        // only the write verbs of this package
    }

    @Override
    public final R run() {
        return tx().requiredCall(tx -> write());
    }

    /** The write itself, inside the transaction; each verb hands it to the method named after it. */
    abstract R write();

    private static TransactionService tx() {
        return StoreTransactions.BEAN.get();
    }

}
