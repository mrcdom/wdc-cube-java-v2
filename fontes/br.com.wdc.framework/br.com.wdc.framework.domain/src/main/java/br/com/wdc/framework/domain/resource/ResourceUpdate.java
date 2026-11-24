package br.com.wdc.framework.domain.resource;

import java.util.function.Supplier;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * Changes some fields of an existing record: not idempotent in general (an increment is a change too).
 * <p>
 * The {@code PATCH} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #update()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceUpdate<R> extends ResourceWrite<R> {

    /** @param transactions the transaction service of the call's module, read when the call runs */
    protected ResourceUpdate(Supplier<TransactionService> transactions) {
        super(transactions);
    }

    @Override
    final R write() {
        return update();
    }

    /** Changes the record, inside the transaction. */
    protected abstract R update();

}
