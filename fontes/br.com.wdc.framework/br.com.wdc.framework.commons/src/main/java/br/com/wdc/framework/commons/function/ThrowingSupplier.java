package br.com.wdc.framework.commons.function;

import java.util.function.Supplier;

import br.com.wdc.framework.commons.util.Rethrow;


public interface ThrowingSupplier<T> extends Supplier<T> {

    @SuppressWarnings("unchecked")
    static <R> ThrowingSupplier<R> noop() {
        return (ThrowingSupplier<R>) ThrowingConsts.NOOP_SUPPLIER;
    }

    @Override
    default T get() {
        try {
            return getThrows();
        } catch (final Exception caught) {
        	throw Rethrow.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    T getThrows() throws Exception;

}
