package br.com.wdc.shopping.test.util;

import java.time.Duration;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class ScheduledExecutorForTestSyncDirect implements ScheduledExecutorForTest {

    @Override
    public Registration execute(ThrowingRunnable command) {
        command.run();
        return Registration.noop();
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        return this.execute(command);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        return this.execute(command);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        return this.execute(command);
    }

    @Override
    public void flush() {
        // NOOP
    }

    @Override
    public void shutdown() {
        // NOOP
    }

}
