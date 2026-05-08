package br.com.wdc.shopping.view.teavm;

import java.time.Duration;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.shopping.view.teavm.interop.Console;
import br.com.wdc.shopping.view.teavm.interop.Timers;

/**
 * Implementação de {@link ScheduledExecutor} para browser via setTimeout/setInterval.
 * Single-threaded — executa tudo na thread principal do browser.
 */
public class ScheduledExecutorBrowser implements ScheduledExecutor {

    @Override
    public Registration execute(ThrowingRunnable command) {
        int id = Timers.setTimeout(() -> runSafe(command), 0);
        return () -> Timers.clearTimeout(id);
    }

    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        int delayMs = (int) delay.toMillis();
        int id = Timers.setTimeout(() -> runSafe(command), delayMs);
        return () -> Timers.clearTimeout(id);
    }

    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        int initialMs = (int) initialDelay.toMillis();
        int periodMs = (int) period.toMillis();
        final int[] intervalId = { 0 };

        int timeoutId = Timers.setTimeout(() -> {
            runSafe(command);
            intervalId[0] = Timers.setInterval(() -> runSafe(command), periodMs);
        }, initialMs);

        return () -> {
            Timers.clearTimeout(timeoutId);
            if (intervalId[0] != 0) {
                Timers.clearInterval(intervalId[0]);
            }
        };
    }

    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        int initialMs = (int) initialDelay.toMillis();
        int delayMs = (int) delay.toMillis();
        final int[] nextId = { 0 };
        final boolean[] cancelled = { false };

        Runnable scheduleNext = new Runnable() {
            @Override
            public void run() {
                if (cancelled[0]) return;
                nextId[0] = Timers.setTimeout(() -> {
                    runSafe(command);
                    this.run();
                }, delayMs);
            }
        };

        int timeoutId = Timers.setTimeout(() -> {
            runSafe(command);
            scheduleNext.run();
        }, initialMs);

        return () -> {
            cancelled[0] = true;
            Timers.clearTimeout(timeoutId);
            Timers.clearTimeout(nextId[0]);
        };
    }

    private static void runSafe(ThrowingRunnable command) {
        try {
            command.run();
        } catch (Exception e) {
            Console.error("ScheduledExecutorBrowser error: " + e.getMessage());
        }
    }

}
