package br.com.wdc.framework.commons.function;

import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.lang3.exception.ExceptionUtils;

public interface ThrowingFunction<T, R> extends Function<T, R> {

    @SuppressWarnings("unchecked")
    static <V, R> ThrowingFunction<V, R> noop() {
        return (ThrowingFunction<V, R>) ThrowingConsts.NOOP_FUNCTION;
    }

    @Override
    default R apply(final T t) {
        try {
            return this.applyThrows(t);
        } catch (final Exception caught) {
        	throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    R applyThrows(T t) throws Exception;

    default <V> ThrowingFunction<V, R> compose(final ThrowingFunction<? super V, ? extends T> before) {
        Objects.requireNonNull(before);
        return (final V v) -> this.apply(before.apply(v));
    }

    default <V> ThrowingFunction<T, V> andThen(final ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (final T t) -> after.apply(this.apply(t));
    }

    static <T> ThrowingFunction<T, T> identityThrows() {
        return t -> t;
    }
}
