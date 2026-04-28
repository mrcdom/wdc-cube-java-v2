package br.com.wdc.framework.commons.function;

import org.apache.commons.lang3.exception.ExceptionUtils;

public interface ThrowingRunnable extends Runnable {

    static ThrowingRunnable noop() {
        return ThrowingConsts.NOOP_RUNNABLE;
    }

    @Override
    default void run() {
        try {
            runThrows();
        } catch (final Exception caught) {
        	throw ExceptionUtils.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    void runThrows() throws Exception;

}
