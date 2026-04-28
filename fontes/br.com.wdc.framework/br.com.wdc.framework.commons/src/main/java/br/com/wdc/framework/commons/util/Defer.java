package br.com.wdc.framework.commons.util;

import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class Defer implements AutoCloseable {

    @SuppressWarnings("java:S3077")
    private volatile ThrowingRunnable actionStack = ThrowingRunnable.noop();

    public void push(ThrowingRunnable action) {
        this.actionStack = Defer.join(action, this.actionStack);
    }

    @Override
    public void close() {
        this.run();
    }

    public void run() {
        try {
            this.actionStack.runThrows();
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        } finally {
            this.actionStack = ThrowingRunnable.noop();
        }
    }

    public void absorb(Defer other) {
        this.actionStack = Defer.join(other.actionStack, this.actionStack);
        other.actionStack = ThrowingRunnable.noop();
    }

    private static ThrowingRunnable join(ThrowingRunnable a, ThrowingRunnable b) {
        return () -> {
            a.runThrows();
            b.runThrows();
        };
    }

}
