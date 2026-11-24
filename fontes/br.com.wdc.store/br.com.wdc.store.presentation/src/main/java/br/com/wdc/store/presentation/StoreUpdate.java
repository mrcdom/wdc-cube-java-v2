package br.com.wdc.store.presentation;

/**
 * Changes some fields of an existing record: not idempotent in general (an increment is a change too).
 * <p>
 * The {@code PATCH} of the calls. {@link #run()} opens the transaction (or joins the caller's) and calls
 * {@link #update()}.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreUpdate<R> extends StoreWrite<R> {

    @Override
    final R write() {
        return update();
    }

    /** Changes the record, inside the transaction. */
    protected abstract R update();

}
