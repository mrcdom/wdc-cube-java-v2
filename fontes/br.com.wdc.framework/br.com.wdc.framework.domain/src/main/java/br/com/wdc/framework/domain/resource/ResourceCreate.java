package br.com.wdc.framework.domain.resource;

import java.util.function.Supplier;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * Creates a new record: not idempotent, each run creates another one, so a run is never repeated on its own.
 * <p>
 * The {@code POST} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #create()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceCreate<R> extends ResourceWrite<R> {

    /** @param transactions the transaction service of the call's module, read when the call runs */
    protected ResourceCreate(Supplier<TransactionService> transactions) {
        super(transactions);
    }

    @Override
    final R write() {
        return create();
    }

    /** Creates the record, inside the transaction. */
    protected abstract R create();

}
