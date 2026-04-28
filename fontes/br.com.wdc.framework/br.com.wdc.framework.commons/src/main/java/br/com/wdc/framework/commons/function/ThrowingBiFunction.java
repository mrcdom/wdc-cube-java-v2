package br.com.wdc.framework.commons.function;

import java.util.Objects;
import java.util.function.BiFunction;

import org.apache.commons.lang3.exception.ExceptionUtils;

public interface ThrowingBiFunction<T, U, R> extends BiFunction<T, U, R> {

    @SuppressWarnings("unchecked")
    static <P0, P1, R> ThrowingBiFunction<P0, P1, R> noop() {
        return (ThrowingBiFunction<P0, P1, R>) ThrowingConsts.NOOP_BIFUNCTION;
    }

    @Override
    default R apply(final T t, final U u) {
        try {
            return this.applyThrows(t, u);
        } catch (final Exception caught) {
        	throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    R applyThrows(T t, U u) throws Exception;

    default <V> ThrowingBiFunction<T, U, V> andThen(final ThrowingFunction<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (final T t, final U u) -> after.apply(this.apply(t, u));
    }

}
