package br.com.wdc.framework.domain.resource;

import java.util.function.Supplier;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * Removes a record: idempotent, removing what is already gone leaves the same state.
 * <p>
 * The {@code DELETE} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #delete()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceDelete<R> extends ResourceWrite<R> {

    /** @param transactions the transaction service of the call's module, read when the call runs */
    protected ResourceDelete(Supplier<TransactionService> transactions) {
        super(transactions);
    }

    @Override
    final R write() {
        return delete();
    }

    /** Removes the record, inside the transaction. */
    protected abstract R delete();

}
