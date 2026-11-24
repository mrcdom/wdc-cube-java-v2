package br.com.wdc.store.presentation;

/**
 * Removes a record: idempotent, removing what is already gone leaves the same state.
 * <p>
 * The {@code DELETE} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #delete()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreDelete<R> extends StoreWrite<R> {

    @Override
    final R write() {
        return delete();
    }

    /** Removes the record, inside the transaction. */
    protected abstract R delete();

}
