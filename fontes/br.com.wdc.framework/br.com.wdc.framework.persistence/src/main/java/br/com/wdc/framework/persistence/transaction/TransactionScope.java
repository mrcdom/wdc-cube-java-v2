package br.com.wdc.framework.persistence.transaction;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import br.com.wdc.framework.domain.transaction.AfterCommitTasks;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;

/**
 * Transaction frame bound to the thread (via {@link ThreadLocal}), operating in two modes:
 *
 * <ul>
 * <li><b>JTA</b> — when {@link JtaTransactionManager#BEAN} is initialized. Delegates begin/commit/rollback to the
 * {@link TransactionManager}; the JDBC connection is obtained from the {@link DataSource} which, being JTA-aware
 * (configured by the host), is automatically enlisted as an XA resource in the current transaction.</li>
 * <li><b>JDBC</b> — when {@link JtaTransactionManager#BEAN} is {@code null} (default). Manages the transaction directly
 * via {@link Connection#commit()}/{@link Connection#rollback()}, with no external TM.</li>
 * </ul>
 *
 * <p>
 * <b>Reentrancy (REQUIRED propagation):</b> {@link #begin(DataSource)} joins the active transaction when there is one —
 * <b>sharing the owner's connection</b> (a single physical transaction in both modes) — or opens a new one and becomes
 * the owner. Only the owner commits/rolls back and closes the connection in {@link #close()}; participants are no-ops.
 * </p>
 *
 * <p>
 * <b>Suspension (REQUIRES_NEW / NOT_SUPPORTED):</b> {@link #suspend()} detaches the current transaction from the thread
 * (JTA: {@code tm.suspend()}; JDBC: the connection/transaction stays alive, with no owner on the thread) and
 * {@link #resume(TransactionScope)} reattaches it. In JDBC mode, the new scope opens a <b>second connection</b> from the
 * pool, independent of the suspended one.
 * </p>
 *
 * <p>
 * Used by the TransactionService (CMT style) and by the repositories, which obtain the current transaction's connection
 * via {@link #connection()}. Single-thread: do not share across threads.
 * </p>
 */
public final class TransactionScope implements AutoCloseable {

    private static final ThreadLocal<TransactionScope> CURRENT = new ThreadLocal<>();

    private final Connection connection;
    private final boolean owner;
    /** The frame that owns the physical transaction: itself, or the one a participant joined. */
    private final TransactionScope root;
    /** Non-null only in JDBC owner mode — original {@code autoCommit} to restore in {@link #close()}. */
    private final Boolean jdbcOldAutoCommit;
    /** Non-null only in JTA owner mode — Narayana transaction, for suspend/resume. */
    private Transaction jtaTransaction;
    private boolean rollbackOnly;
    private boolean closed;
    /** Tasks for after the commit; only the owner's queue counts (participants hand them to it). */
    private final AfterCommitTasks afterCommitTasks = new AfterCommitTasks();
    /** Whether the owner finished with a commit that succeeded. */
    private boolean committed;

    private TransactionScope(Connection connection, TransactionScope root, Boolean jdbcOldAutoCommit) {
        this.connection = connection;
        this.owner = root == null;
        this.root = root == null ? this : root;
        this.jdbcOldAutoCommit = jdbcOldAutoCommit;
    }

    // :: REQUIRED propagation

    /**
     * Joins the active transaction (participant sharing the owner's connection) or opens a new one (owner).
     */
    public static TransactionScope begin(DataSource dataSource) throws SQLException, SystemException {
        var current = CURRENT.get();
        if (current != null && !current.closed) {
            // Participant: shares the owner's connection (and therefore transaction). No lifecycle of its own.
            return new TransactionScope(current.connection, current.root, null);
        }
        return beginNew(dataSource);
    }

    /**
     * Unconditionally opens a new owner transaction and installs it as current. Must be preceded by
     * {@link #suspend()} if a transaction is already active (REQUIRES_NEW case).
     */
    public static TransactionScope beginNew(DataSource dataSource) throws SQLException, SystemException {
        var tm = JtaTransactionManager.BEAN.get();
        return tm != null ? beginJta(dataSource, tm) : beginJdbc(dataSource);
    }

    private static TransactionScope beginJta(DataSource dataSource, TransactionManager tm)
            throws SQLException, SystemException {
        try {
            tm.begin();
        } catch (jakarta.transaction.NotSupportedException e) {
            throw new SystemException("Could not start the JTA transaction: " + e.getMessage());
        }
        Connection connection;
        Transaction jtaTx;
        try {
            connection = dataSource.getConnection(); // JTA-aware DataSource enlists the connection in the tm
            jtaTx = tm.getTransaction();
        } catch (SQLException | SystemException e) {
            try {
                tm.rollback();
            } catch (SystemException suppressed) {
                e.addSuppressed(suppressed);
            }
            throw e;
        }
        var ctx = new TransactionScope(connection, null, null);
        ctx.jtaTransaction = jtaTx;
        CURRENT.set(ctx);
        return ctx;
    }

    private static TransactionScope beginJdbc(DataSource dataSource) throws SQLException {
        var connection = dataSource.getConnection();
        var oldAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        var ctx = new TransactionScope(connection, null, oldAutoCommit);
        CURRENT.set(ctx);
        return ctx;
    }

    // :: Suspend / Resume (REQUIRES_NEW, NOT_SUPPORTED)

    /**
     * Detaches the current transaction from the thread and returns it for a later {@link #resume(TransactionScope)}.
     * Returns {@code null} if there is no active transaction. The suspended transaction stays alive (it is not
     * committed).
     */
    public static TransactionScope suspend() {
        var current = CURRENT.get();
        if (current == null || current.closed) {
            return null;
        }
        CURRENT.remove();
        var tm = JtaTransactionManager.BEAN.get();
        if (tm != null) {
            try {
                tm.suspend(); // delists the JTA tx from the thread; current.jtaTransaction keeps the reference
            } catch (SystemException e) {
                throw new IllegalStateException("Failed to suspend the JTA transaction", e);
            }
        }
        return current;
    }

    /**
     * Reattaches a transaction previously suspended by {@link #suspend()}. No-op if {@code suspended} is {@code null}.
     */
    public static void resume(TransactionScope suspended) {
        if (suspended == null) {
            return;
        }
        var tm = JtaTransactionManager.BEAN.get();
        if (tm != null) {
            try {
                tm.resume(suspended.jtaTransaction);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to resume the JTA transaction", e);
            }
        }
        CURRENT.set(suspended);
    }

    // :: Access / state

    /** The thread's current context, or {@code null} if there is no active transaction. */
    public static TransactionScope current() {
        var current = CURRENT.get();
        return (current != null && !current.closed) ? current : null;
    }

    /** Checks whether there is an active transaction on the current thread. */
    public static boolean isActive() {
        return current() != null;
    }

    /**
     * Gets the current transaction's connection.
     *
     * @throws IllegalStateException if the context has already been closed
     */
    public Connection connection() {
        if (closed) {
            throw new IllegalStateException("TransactionScope already closed");
        }
        return connection;
    }

    public boolean isRollbackOnly() {
        if (owner) {
            return rollbackOnly;
        }
        var cur = current();
        return cur != null && cur.rollbackOnly;
    }

    /** Marks the current transaction for rollback (one-way). The commit is not executed on the owner's close. */
    public void setRollbackOnly() {
        if (owner) {
            this.rollbackOnly = true;
        } else {
            var current = CURRENT.get();
            if (current != null) {
                current.rollbackOnly = true;
            }
        }
    }

    /** Whether this frame owns the physical transaction (commits or rolls back in {@link #close()}). */
    public boolean ownsTransaction() {
        return owner;
    }

    /**
     * Schedules {@code task} for after the commit of this physical transaction (the owner's, also when called on a
     * participant). Whoever closes the owner runs it, through {@link #takeAfterCommit()}.
     *
     * @throws IllegalStateException if the transaction is already closed
     */
    public void afterCommit(Runnable task) {
        if (root.closed) {
            throw new IllegalStateException("TransactionScope already closed");
        }
        root.afterCommitTasks.add(task);
    }

    /**
     * The scheduled tasks, if the owner was closed with a commit; empty if it was rolled back, is still open, or this
     * is a participant. Drains the queue: each task is delivered only once.
     */
    public List<Runnable> takeAfterCommit() {
        if (!owner || !closed) {
            return List.of();
        }
        var tasks = afterCommitTasks.drain();
        return committed ? tasks : List.of();
    }

    // :: Closing

    /**
     * Closes the frame. Participant: no-op (the connection belongs to the owner). Owner: commits (or rolls back if
     * marked) and closes the connection.
     */
    @Override
    public void close() throws SQLException {
        if (closed) {
            return;
        }
        closed = true;

        if (!owner) {
            // Participant: shares the owner's connection — nothing to close/finish.
            return;
        }

        CURRENT.remove();

        if (jdbcOldAutoCommit == null) {
            // JTA: closes the connection handle (the pool only recycles it when the transaction ends) and finishes via the TM.
            silentClose(connection);
            commitOrRollbackJta();
        } else {
            // JDBC: commits/rolls back via the connection and closes it.
            commitOrRollbackJdbc();
        }
    }

    // -------------------------------------------------------------------------

    private void commitOrRollbackJta() throws SQLException {
        var tm = JtaTransactionManager.get();
        try {
            if (rollbackOnly || isMarkedRollback(tm)) {
                tm.rollback();
            } else {
                tm.commit();
                committed = true;
            }
        } catch (Exception e) {
            throw new SQLException("Failed to finish the JTA transaction", e);
        }
    }

    private void commitOrRollbackJdbc() throws SQLException {
        try {
            if (rollbackOnly) {
                connection.rollback();
            } else {
                connection.commit();
                committed = true;
            }
        } catch (SQLException ex) {
            try {
                connection.rollback();
            } catch (SQLException suppressed) {
                ex.addSuppressed(suppressed);
            }
            throw ex;
        } finally {
            try {
                connection.setAutoCommit(jdbcOldAutoCommit);
            } catch (SQLException _ignore) {
                // best effort
            }
            silentClose(connection);
        }
    }

    private static void silentClose(Connection connection) {
        try {
            connection.close();
        } catch (Exception __ignore) {
            // best effort
        }
    }

    private static boolean isMarkedRollback(TransactionManager tm) {
        try {
            return tm.getStatus() == Status.STATUS_MARKED_ROLLBACK;
        } catch (SystemException _ignore) {
            return false;
        }
    }
}
