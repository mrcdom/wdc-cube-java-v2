package br.com.wdc.shopping.test.util;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

/**
 * Async test executor backed by Virtual Threads.
 *
 * <p>
 * One-shot tasks ({@link #execute} and {@link #schedule}) are submitted to a virtual-thread executor and their futures
 * are tracked so that {@link #flush()} can block until all of them complete. Scheduled delays are honoured by sleeping
 * the virtual thread rather than using a timer, which avoids race conditions.
 * </p>
 *
 * <p>
 * Periodic tasks ({@link #scheduleAtFixedRate} / {@link #scheduleWithFixedDelay}) are managed separately via a
 * single-platform-thread timer that spawns virtual threads for each execution. They are intentionally <em>not</em>
 * tracked so that {@link #flush()} does not wait for them indefinitely.
 * </p>
 */
public class ScheduledExecutorForTestAsync implements ScheduledExecutorForTest {

    // Single platform-thread scheduler – used only to fire periodic tasks at the right time.
    private final ScheduledExecutorService timer;

    // Virtual-thread executor for one-shot tasks.
    private final ExecutorService vtExecutor;

    // Tracked futures for one-shot tasks so flush() can await them.
    private List<Future<?>> pendingFutures = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean running = true;

    public ScheduledExecutorForTestAsync() {
        this.timer = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().daemon(true).name("test-async-timer").factory());
        this.vtExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    // -------------------------------------------------------------------------
    // ScheduledExecutorForTest

    @Override
    public void shutdown() {
        this.running = false;
        this.timer.shutdownNow();
        this.vtExecutor.shutdownNow();
    }

    /**
     * Blocks until all one-shot tasks submitted since the last {@code flush()} have finished. Swaps the pending list
     * atomically so new tasks submitted concurrently go into the next batch and are not lost.
     */
    @Override
    public void flush() throws Exception {
        if (!running)
            return;

        // Atomically swap out the pending list so that execute() calls arriving while we are
        // waiting are safely captured by the new list.
        var batch = this.pendingFutures;
        this.pendingFutures = Collections.synchronizedList(new ArrayList<>());

        for (var future : batch) {
            future.get();
        }
    }

    // -------------------------------------------------------------------------
    // ScheduledExecutor

    @Override
    public Registration execute(ThrowingRunnable command) {
        var allowed = new AtomicBoolean(true);
        var future = this.vtExecutor.submit(() -> {
            if (allowed.get() && running) {
                command.run();
            }
            return null;
        });
        this.pendingFutures.add(future);
        return () -> allowed.set(false);
    }

    /**
     * Submits a virtual thread that sleeps for {@code delay} and then runs the command. The future is tracked so that
     * {@code flush()} will wait for it.
     */
    @Override
    public Registration schedule(ThrowingRunnable command, Duration delay) {
        var allowed = new AtomicBoolean(true);
        var future = this.vtExecutor.submit(() -> {
            if (allowed.get() && running) {
                Thread.sleep(delay);
                command.run();
            }
            return null;
        });
        this.pendingFutures.add(future);
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
                        } catch (Throwable ignored) {
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
                        } catch (Throwable ignored) {
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
