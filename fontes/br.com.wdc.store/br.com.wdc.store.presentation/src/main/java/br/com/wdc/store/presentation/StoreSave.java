package br.com.wdc.store.presentation;

import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.store.domain.StoreTransactions;

/**
 * A write: {@link #run()} opens the transaction (or joins the caller's) and does the work in {@link #save()}, so no
 * write goes out of one.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreSave<R> extends StoreCall<R> {

    @Override
    public final R run() {
        return tx().requiredCall(tx -> save());
    }

    /** The write itself, inside the transaction. */
    protected abstract R save();

    private static TransactionService tx() {
        return StoreTransactions.BEAN.get();
    }

}
