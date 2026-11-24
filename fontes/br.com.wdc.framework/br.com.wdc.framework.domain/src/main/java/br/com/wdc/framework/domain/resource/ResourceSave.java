package br.com.wdc.framework.domain.resource;

import java.util.function.Supplier;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * Writes a record whole, creating it or replacing it: idempotent, the same request twice leaves the same state.
 * <p>
 * The {@code PUT} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #save()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceSave<R> extends ResourceWrite<R> {

    /** @param transactions the transaction service of the call's module, read when the call runs */
    protected ResourceSave(Supplier<TransactionService> transactions) {
        super(transactions);
    }

    @Override
    final R write() {
        return save();
    }

    /** Writes the record whole, inside the transaction. */
    protected abstract R save();

}
