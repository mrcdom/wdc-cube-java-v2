package br.com.wdc.shopping.view.swt;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.widgets.Display;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * Adapter that schedules tasks on a ScheduledExecutorService but ensures
 * execution happens on the SWT UI thread.
 */
public class ScheduledExecutorSwtAdapter implements ScheduledExecutor {

    private final ScheduledExecutorService service;
    private final Display display;

    public ScheduledExecutorSwtAdapter(ScheduledExecutorService service, Display display) {
        this.service = service;
        this.display = display;
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var future = this.service.schedule(() -> this.display.asyncExec(command), 0, TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var future = this.service.schedule(() -> this.display.asyncExec(command), delay.toMillis(),
                TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var future = this.service.scheduleAtFixedRate(() -> this.display.asyncExec(command),
                initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var future = this.service.scheduleWithFixedDelay(() -> this.display.asyncExec(command),
                initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }
}
