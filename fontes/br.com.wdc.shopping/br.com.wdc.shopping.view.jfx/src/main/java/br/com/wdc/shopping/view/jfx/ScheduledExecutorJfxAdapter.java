package br.com.wdc.shopping.view.jfx;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;
import javafx.application.Platform;

/**
 * Adapter that schedules tasks on a ScheduledExecutorService but ensures execution happens on the JavaFX Application
 * Thread.
 */
public class ScheduledExecutorJfxAdapter implements ScheduledExecutor {

    private final ScheduledExecutorService service;

    public ScheduledExecutorJfxAdapter(ScheduledExecutorService service) {
        this.service = service;
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var future = this.service.schedule(() -> Platform.runLater(command), 0, TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var future = this.service.schedule(() -> Platform.runLater(command), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var future = this.service.scheduleAtFixedRate(() -> Platform.runLater(command),
                initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var future = this.service.scheduleWithFixedDelay(() -> Platform.runLater(command),
                initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }
}
