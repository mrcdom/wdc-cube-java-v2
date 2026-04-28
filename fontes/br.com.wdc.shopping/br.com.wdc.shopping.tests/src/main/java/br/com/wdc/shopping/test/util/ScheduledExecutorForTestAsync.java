package br.com.wdc.shopping.test.util;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * Async test executor backed by Virtual Threads and StructuredTaskScope.
 *
 * <p>
 * One-shot tasks ({@link #execute} and {@link #schedule}) are forked into a {@link StructuredTaskScope} so that
 * {@link #flush()} can block until all of them complete by calling {@code scope.join()}. Scheduled delays are honoured
 * by sleeping the virtual thread rather than using a timer, which avoids race conditions between the timer thread and
 * the scope lifecycle.
 * </p>
 *
 * <p>
 * Periodic tasks ({@link #scheduleAtFixedRate} / {@link #scheduleWithFixedDelay}) are managed separately via a
 * single-platform-thread timer that spawns virtual threads for each execution. They are intentionally <em>not</em>
 * tracked by the scope so that {@link #flush()} does not wait for them indefinitely.
 * </p>
 */
public class ScheduledExecutorForTestAsync implements ScheduledExecutorForTest {

    // Single platform-thread scheduler – used only to fire periodic tasks at the right time.
    private final ScheduledExecutorService timer;

    // Current scope for one-shot tasks. Replaced atomically on each flush().
    @SuppressWarnings("java:S3077")
    private volatile StructuredTaskScope<Object, Void> scope;

    private volatile boolean running = true;

    public ScheduledExecutorForTestAsync() {
        this.timer = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon(true).name("test-async-timer").factory());
        this.scope = openScope();
    }

    private static StructuredTaskScope<Object, Void> openScope() {
        return (StructuredTaskScope<Object, Void>) StructuredTaskScope.open(Joiner.awaitAllSuccessfulOrThrow());
    }

    // -------------------------------------------------------------------------
    // ScheduledExecutorForTest

    @Override
    public void shutdown() {
        this.running = false;
        this.timer.shutdownNow();
        try {
            this.scope.close();
        } catch (Exception _) {
            // best-effort cleanup
        }
    }

    /**
     * Blocks until all one-shot tasks forked since the last {@code flush()} have finished. Replaces the scope
     * atomically so new tasks submitted concurrently go into the next batch and are not lost.
     */
    @Override
    public void flush() throws Exception {
        if (!running)
            return;

        // Atomically swap out the scope so that execute() calls arriving while we are
        // joining the old scope are safely captured by the new one.
        var oldScope = this.scope;
        this.scope = openScope();

        oldScope.join();
        oldScope.close();
    }

    // -------------------------------------------------------------------------
    // ScheduledExecutor

    @Override
    public Registration execute(ThrowingRunnable command) {
        var allowed = new AtomicBoolean(true);
        this.scope.fork(() -> {
            if (allowed.get() && running) {
                command.run();
            }
            return null;
        });
        return () -> allowed.set(false);
    }

    /**
     * Forks a virtual thread that sleeps for {@code delay} and then runs the command. This integrates naturally with
     * {@link #flush()}: the sleeping virtual thread is tracked by the scope, so {@code flush()} will wait for it.
     */
    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var allowed = new AtomicBoolean(true);
        this.scope.fork(() -> {
            if (allowed.get() && running) {
                Thread.sleep(delay);
                command.run();
            }
            return null;
        });
        return () -> allowed.set(false);
    }

    /**
     * Periodic task – NOT tracked by the scope; {@link #flush()} does not wait for it. Each execution runs on its own
     * virtual thread.
     */
    @Override
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period) {
        var allowed = new AtomicBoolean(true);

        @SuppressWarnings("java:S1181")
        Future<?> future = this.timer.scheduleAtFixedRate(
                () -> {
                    if (!running || !allowed.get()) {
                        return;
                    }

                    Thread.startVirtualThread(() -> {
                        try {
                            if (running && allowed.get()) {
                                command.run();
                            }
                        } catch (Throwable _) {
                            // periodic task failures are intentionally swallowed here
                        }
                    });
                },
                initialDelay.toMillis(),
                period.toMillis(),
                TimeUnit.MILLISECONDS);

        return () -> {
            allowed.set(false);
            future.cancel(false);
        };
    }

    /**
     * Periodic task – NOT tracked by the scope; {@link #flush()} does not wait for it. Each execution runs on its own
     * virtual thread.
     */
    @Override
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay) {
        var allowed = new AtomicBoolean(true);

        @SuppressWarnings("java:S1181")
        Future<?> future = this.timer.scheduleWithFixedDelay(
                () -> {
                    if (!running || !allowed.get()) {
                        return;
                    }

                    Thread.startVirtualThread(() -> {
                        try {
                            if (running && allowed.get()) {
                                command.run();
                            }
                        } catch (Throwable _) {
                            // periodic task failures are intentionally swallowed here
                        }
                    });
                },
                initialDelay.toMillis(),
                delay.toMillis(),
                TimeUnit.MILLISECONDS);

        return () -> {
            allowed.set(false);
            future.cancel(false);
        };
    }

}
