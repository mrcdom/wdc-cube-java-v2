package br.com.wdc.shopping.persistence.concurrent;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class ScheduledExecutorAdapter implements ScheduledExecutor {

    private ScheduledExecutorService service;

    public ScheduledExecutorAdapter(ScheduledExecutorService service) {
        this.service = service;
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var future = service.schedule(command, 0, TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var future = service.schedule(command, delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var future = service.scheduleAtFixedRate(command, initialDelay.toMillis(), period.toMillis(),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var future = service.scheduleWithFixedDelay(command, initialDelay.toMillis(), delay.toMillis(),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

}
