package br.com.wdc.shopping.view.robovm;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.robovm.apple.foundation.NSOperationQueue;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * Adapter that schedules tasks on a ScheduledExecutorService but ensures
 * execution happens on the iOS main thread via NSOperationQueue.
 */
public class ScheduledExecutorRoboVMAdapter implements ScheduledExecutor {

    private final ScheduledExecutorService service;

    public ScheduledExecutorRoboVMAdapter(ScheduledExecutorService service) {
        this.service = service;
    }

    private static void runOnMainThread(ThrowingRunnable command) {
        NSOperationQueue.getMainQueue().addOperation(() -> {
            try {
                command.run();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var future = this.service.schedule(() -> runOnMainThread(command), 0, TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var future = this.service.schedule(() -> runOnMainThread(command), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var future = this.service.scheduleAtFixedRate(() -> runOnMainThread(command),
                initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var future = this.service.scheduleWithFixedDelay(() -> runOnMainThread(command),
                initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }
}
