package br.com.wdc.framework.commons.concurrent;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;

public interface ScheduledExecutor {
	
	AtomicReference<ScheduledExecutor> BEAN = new AtomicReference<>();

    /**
     * Executes the given command at some time in the future. The command may execute in a new thread, in a pooled
     * thread, or in the calling thread, at the discretion of the {@code Executor} implementation.
     *
     * @param command the runnable task
     * @return a Registration that allow to cancel request if yet not executed
     * @throws RejectedExecutionException if this task cannot be accepted for execution
     * @throws NullPointerException       if command is null
     */
    Registration execute(ThrowingRunnable command);

    /**
     * Submits a one-shot task that becomes enabled after the given delay.
     *
     * @param command the task to execute
     * @param delay   the time from now to delay execution
     * @return a Promise representing pending completion of the task and whose {@code get()} method will return
     *         {@code null} upon completion
     * @throws NullPointerException if command or unit is null
     */
    Registration schedule(ThrowingRunnable command, Duration delay);

    /**
     * Submits a periodic action that becomes enabled first after the given initial delay, and subsequently with the
     * given period; that is, executions will commence after {@code initialDelay}, then {@code initialDelay + period},
     * then {@code initialDelay + 2 * period}, and so on.
     *
     * <p>
     * The sequence of task executions continues indefinitely until one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled} via the returned future.
     * <li>The executor terminates, also resulting in task cancellation.
     * <li>An execution of the task throws an exception. In this case calling {@link Future#get() get} on the returned
     * future will throw {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed. Subsequent calls to {@link Future#isDone isDone()} on the returned future
     * will return {@code true}.
     *
     * <p>
     * If any execution of this task takes longer than its period, then subsequent executions may start late, but will
     * not concurrently execute.
     *
     * @param command      the task to execute
     * @param initialDelay the time to delay first execution
     * @param period       the period between successive executions
     * @return a Registration that allow to cancel request if yet not executed
     * @throws NullPointerException     if command or unit is null
     * @throws IllegalArgumentException if period less than or equal to zero
     */
    public Registration scheduleAtFixedRate(ThrowingRunnable command, Duration initialDelay, Duration period);

    /**
     * Submits a periodic action that becomes enabled first after the given initial delay, and subsequently with the
     * given delay between the termination of one execution and the commencement of the next.
     *
     * <p>
     * The sequence of task executions continues indefinitely until one of the following exceptional completions occur:
     * <ul>
     * <li>The task is {@linkplain Future#cancel explicitly cancelled} via the returned future.
     * <li>The executor terminates, also resulting in task cancellation.
     * <li>An execution of the task throws an exception. In this case calling {@link Future#get() get} on the returned
     * future will throw {@link ExecutionException}, holding the exception as its cause.
     * </ul>
     * Subsequent executions are suppressed. Subsequent calls to {@link Future#isDone isDone()} on the returned future
     * will return {@code true}.
     *
     * @param command      the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay        the delay between the termination of one execution and the commencement of the next
     * @return a Registration that allow to cancel request if yet not executed
     * @throws NullPointerException     if command or unit is null
     * @throws IllegalArgumentException if delay less than or equal to zero
     */
    public Registration scheduleWithFixedDelay(ThrowingRunnable command, Duration initialDelay, Duration delay);

}
