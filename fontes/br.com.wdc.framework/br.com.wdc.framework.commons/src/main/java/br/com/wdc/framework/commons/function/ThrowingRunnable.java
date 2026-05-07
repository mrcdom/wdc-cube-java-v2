package br.com.wdc.framework.commons.function;

import br.com.wdc.framework.commons.util.Rethrow;

public interface ThrowingRunnable extends Runnable {

    static ThrowingRunnable noop() {
        return ThrowingConsts.NOOP_RUNNABLE;
    }

    @Override
    default void run() {
        try {
            runThrows();
        } catch (final Exception caught) {
        	throw Rethrow.asRuntimeException(caught);
        }
    }

    @SuppressWarnings("java:S112")
    void runThrows() throws Exception;

}
