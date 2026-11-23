package br.com.wdc.shopping.presentation;

import br.com.wdc.framework.domain.transaction.TransactionService;
import br.com.wdc.shopping.domain.ShoppingTransactions;

/**
 * A write: {@link #run()} opens the transaction (or joins the caller's) and does the work in {@link #save()}, so no
 * write goes out of one.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ShoppingSave<R> extends ShoppingCall<R> {

    @Override
    public final R run() {
        return tx().requiredCall(tx -> save());
    }

    /** The write itself, inside the transaction. */
    protected abstract R save();

    private static TransactionService tx() {
        return ShoppingTransactions.BEAN.get();
    }

}
