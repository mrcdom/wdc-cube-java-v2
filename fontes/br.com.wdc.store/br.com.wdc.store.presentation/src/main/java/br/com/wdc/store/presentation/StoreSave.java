package br.com.wdc.store.presentation;

/**
 * Writes a record whole, creating it or replacing it: idempotent, the same request twice leaves the same state.
 * <p>
 * The {@code PUT} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #save()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreSave<R> extends StoreWrite<R> {

    @Override
    final R write() {
        return save();
    }

    /** Writes the record whole, inside the transaction. */
    protected abstract R save();

}
