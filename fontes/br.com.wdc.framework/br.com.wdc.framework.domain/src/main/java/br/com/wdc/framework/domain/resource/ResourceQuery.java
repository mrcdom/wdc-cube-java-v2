package br.com.wdc.framework.domain.resource;

/**
 * A read: no side effects, safe to repeat, no transaction of its own (it reads in the caller's, if there is one).
 * <p>
 * The {@code GET} (or {@code QUERY}) of the calls.
 *
 * @param <R> the answer of {@link #run()}
 */
public abstract class ResourceQuery<R> extends ResourceCall<R> {

}
