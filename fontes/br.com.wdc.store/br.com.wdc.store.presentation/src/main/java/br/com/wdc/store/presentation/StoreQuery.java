package br.com.wdc.store.presentation;

/**
 * A read: no side effects, safe to repeat, no transaction of its own (it reads in the caller's, if there is one).
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class StoreQuery<R> extends StoreCall<R> {

}
