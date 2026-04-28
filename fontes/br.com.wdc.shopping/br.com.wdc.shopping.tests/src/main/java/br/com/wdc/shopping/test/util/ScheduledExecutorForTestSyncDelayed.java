package br.com.wdc.shopping.test.util;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class ScheduledExecutorForTestSyncDelayed implements ScheduledExecutorForTest {

    private int sequenceGenerator;

    private int lastExecutedSequence;

    private Map<Integer, ThrowingRunnable> commandMap;

    public ScheduledExecutorForTestSyncDelayed() {
        this.commandMap = new ConcurrentHashMap<>();
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var sequenceId = Integer.valueOf(this.sequenceGenerator++);
        this.commandMap.put(sequenceId, command);
        return () -> this.commandMap.remove(sequenceId);
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
    public void flush() throws Exception {
        for (int i = this.lastExecutedSequence; i < this.sequenceGenerator; i++) {
            this.lastExecutedSequence = i + 1;
            var cmd = this.commandMap.remove(i);
            if (cmd != null) {
                cmd.runThrows();
            }

        }
    }

    @Override
    public void shutdown() {
        this.commandMap.clear();
    }

}
