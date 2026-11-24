package br.com.wdc.store.presentation;

/**
 * Creates a new record: not idempotent, each run creates another one, so a run is never repeated on its own.
 * <p>
 * The {@code POST} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #create()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreCreate<R> extends StoreWrite<R> {

    @Override
    final R write() {
        return create();
    }

    /** Creates the record, inside the transaction. */
    protected abstract R create();

}
