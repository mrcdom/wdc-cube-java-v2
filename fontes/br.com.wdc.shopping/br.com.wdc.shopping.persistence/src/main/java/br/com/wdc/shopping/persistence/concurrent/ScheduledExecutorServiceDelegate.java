package br.com.wdc.shopping.persistence.concurrent;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ScheduledExecutorServiceDelegate implements ScheduledExecutorService {

    private ScheduledExecutorService impl;

    public ScheduledExecutorServiceDelegate() {
        impl = ScheduledExecutorServiceUnavailable.INSTANCE;
    }

    public void setImpl(ScheduledExecutorService impl) {
        this.impl = impl != null ? impl : ScheduledExecutorServiceUnavailable.INSTANCE;
    }

    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return impl.schedule(command, delay, unit);
    }

    public void execute(Runnable command) {
        impl.execute(command);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return impl.schedule(callable, delay, unit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return impl.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public void shutdown() {
        impl.shutdown();
    }

    public List<Runnable> shutdownNow() {
        return impl.shutdownNow();
    }

    public boolean isShutdown() {
        return impl.isShutdown();
    }

    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return impl.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public boolean isTerminated() {
        return impl.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return impl.awaitTermination(timeout, unit);
    }

    public <T> Future<T> submit(Callable<T> task) {
        return impl.submit(task);
    }

    public <T> Future<T> submit(Runnable task, T result) {
        return impl.submit(task, result);
    }

    public Future<?> submit(Runnable task) {
        return impl.submit(task);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return impl.invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return impl.invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return impl.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return impl.invokeAny(tasks, timeout, unit);
    }

    private static class ScheduledExecutorServiceUnavailable implements ScheduledExecutorService {

        private static ScheduledExecutorServiceUnavailable INSTANCE = new ScheduledExecutorServiceUnavailable();

        private RuntimeException newUnavailableException() {
            return new RuntimeException("Service unavailable");
        }

        @Override
        public void shutdown() {
            throw newUnavailableException();
        }

        @Override
        public List<Runnable> shutdownNow() {
            throw newUnavailableException();
        }

        @Override
        public boolean isShutdown() {
            return true;
        }

        @Override
        public boolean isTerminated() {
            throw newUnavailableException();
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            throw newUnavailableException();
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            return CompletableFuture.failedFuture(newUnavailableException());
        }

        @Override
        public <T> Future<T> submit(Runnable task, T result) {
            return CompletableFuture.failedFuture(newUnavailableException());
        }

        @Override
        public Future<?> submit(Runnable task) {
            return CompletableFuture.failedFuture(newUnavailableException());
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return List.of(CompletableFuture.failedFuture(newUnavailableException()));
        }

        @Override
        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException {
            return List.of(CompletableFuture.failedFuture(newUnavailableException()));
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks)
                throws InterruptedException, ExecutionException {
            throw newUnavailableException();
        }

        @Override
        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            throw newUnavailableException();
        }

        @Override
        public void execute(Runnable command) {
            throw newUnavailableException();
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
            return new ScheduledFutureWrap(CompletableFuture.failedFuture(newUnavailableException()));
        }

        @Override
        public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
            return new ScheduledFutureWrap<V>(CompletableFuture.failedFuture(newUnavailableException()));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
            return new ScheduledFutureWrap(CompletableFuture.failedFuture(newUnavailableException()));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay,
                TimeUnit unit) {
            return new ScheduledFutureWrap(CompletableFuture.failedFuture(newUnavailableException()));
        }

    }

    private static class ScheduledFutureWrap<T> implements ScheduledFuture<T> {

        private CompletableFuture<T> impl;

        ScheduledFutureWrap(CompletableFuture<T> impl) {
            this.impl = impl;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(Delayed o) {
            if (o == null) {
                return 0;
            }
            return o.getDelay(TimeUnit.NANOSECONDS) == 0 ? 0 : -1;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return impl.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return impl.isCancelled();
        }

        @Override
        public boolean isDone() {
            return impl.isDone();
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return impl.get();
        }

        @Override
        public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return impl.get(timeout, unit);
        }

    }

}
