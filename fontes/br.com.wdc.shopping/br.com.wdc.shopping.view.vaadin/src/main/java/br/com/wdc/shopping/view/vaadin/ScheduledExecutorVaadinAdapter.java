package br.com.wdc.shopping.view.vaadin;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vaadin.flow.component.UI;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public class ScheduledExecutorVaadinAdapter implements ScheduledExecutor {

    private final ScheduledExecutorService service;
    private final UI ui;

    public ScheduledExecutorVaadinAdapter(ScheduledExecutorService service, UI ui) {
        this.service = service;
        this.ui = ui;
    }

    @Override
    public Registration execute(ThrowingRunnable command) {
        var future = this.service.schedule(() -> this.ui.access(command::run), 0, TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var future = this.service.schedule(() -> this.ui.access(command::run), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var future = this.service.scheduleAtFixedRate(() -> this.ui.access(command::run),
                initialDelay.toMillis(), period.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var future = this.service.scheduleWithFixedDelay(() -> this.ui.access(command::run),
                initialDelay.toMillis(), delay.toMillis(), TimeUnit.MILLISECONDS);
        return () -> future.cancel(true);
    }
}
