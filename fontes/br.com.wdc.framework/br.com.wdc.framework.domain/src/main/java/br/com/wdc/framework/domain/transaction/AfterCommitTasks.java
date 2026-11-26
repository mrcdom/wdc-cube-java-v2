package br.com.wdc.framework.domain.transaction;

import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.log.Log;

/**
 * The queue of {@link TransactionService#afterCommit} tasks of a physical transaction, and the common way to run them.
 *
 * <p>
 * Implementations keep one queue per physical transaction, call {@link #add} while it is open and, after the commit
 * (with the connection returned and outside the transaction), {@link #run}. If the transaction is rolled back, they
 * just discard the queue.
 * </p>
 *
 * <p>
 * It is not thread-safe, and does not need to be: a transaction is used by one thread at a time, and the handoff of a
 * remote transaction from one request thread to the next goes through the coordinator's registry.
 * </p>
 */
public final class AfterCommitTasks {

    private static final Log LOG = Log.getLogger(AfterCommitTasks.class);

    private final List<Runnable> tasks = new ArrayList<>();

    /** Appends a task to the end of the queue. */
    public void add(Runnable task) {
        tasks.add(task);
    }

    /** Empties the queue and returns what was there, in scheduling order. */
    public List<Runnable> drain() {
        var drained = List.copyOf(tasks);
        tasks.clear();
        return drained;
    }

    /** Empties the queue and runs what was there (see {@link #runAll}). */
    public void run() {
        runAll(drain());
    }

    /**
     * Runs the tasks in order. A failure goes to the log and does not stop the following ones: the commit has already
     * happened, and rethrowing would make the caller think the write failed.
     */
    public static void runAll(List<Runnable> tasks) {
        for (var task : tasks) {
            try {
                task.run();
            } catch (RuntimeException caught) {
                LOG.error("A task scheduled for after the commit failed", caught);
            }
        }
    }
}
