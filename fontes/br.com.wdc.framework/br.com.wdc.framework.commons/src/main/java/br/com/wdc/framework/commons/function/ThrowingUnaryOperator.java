package br.com.wdc.framework.commons.function;

import java.util.function.UnaryOperator;

import br.com.wdc.framework.commons.util.Rethrow;


public interface ThrowingUnaryOperator<T> extends UnaryOperator<T> {

    @SuppressWarnings("unchecked")
    static <V> ThrowingUnaryOperator<V> noop() {
        return (ThrowingUnaryOperator<V>) ThrowingConsts.NOOP_UNARY_OPERATOR;
    }

    @Override
    default T apply(final T t) {
        try {
            return this.applyThrows(t);
        } catch (final Exception caught) {
        	throw Rethrow.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    T applyThrows(T t) throws Exception;

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * @param <T> the type of the input and output of the operator
     * @return a unary operator that always returns its input argument
     */
    static <T> ThrowingUnaryOperator<T> identity() {
        return t -> t;
    }
}
