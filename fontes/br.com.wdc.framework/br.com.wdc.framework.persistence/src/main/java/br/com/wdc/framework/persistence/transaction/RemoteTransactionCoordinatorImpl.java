package br.com.wdc.framework.persistence.transaction;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import javax.sql.DataSource;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.TransactionConflictException;
import br.com.wdc.framework.domain.exception.TransactionLimitExceededException;
import br.com.wdc.framework.domain.transaction.AfterCommitTasks;
import br.com.wdc.framework.domain.transaction.TransactionSystemException;
import br.com.wdc.framework.domain.transaction.UnknownTransactionException;

/**
 * Remote transaction coordinator (server side), built on {@link TransactionScope}.
 *
 * <p>
 * Each {@code txId} holds a <b>suspended</b> {@link TransactionScope} (live physical transaction, with no owning
 * thread). On each request that carries the {@code txId}, {@link #resume(String, String)} reattaches the scope to the
 * current thread and {@link #suspend(String)} detaches it at the end, keeping it alive for the next one.
 * {@link #commit(String, String)}/{@link #rollback(String, String)} finish and remove it.
 * </p>
 *
 * <p>
 * Works for both {@code TransactionScope} modes: JDBC (single connection kept open) and JTA (suspend/resume on the
 * TransactionManager). The {@code afterCommit} tasks scheduled by the requests of a remote transaction run when its
 * commit succeeds, after the connection is returned. Transactions idle beyond the timeout are rolled back and removed (lazy sweep in
 * {@link #begin(String)}), preventing connection leaks from clients that abandon the transaction.
 * </p>
 */
public final class RemoteTransactionCoordinatorImpl implements RemoteTransactionCoordinator {

    /** Maximum idle time (ms) before a remote transaction is rolled back and removed. */
    public static final long DEFAULT_IDLE_TIMEOUT_MS = 60_000L;
    /**
     * <b>Absolute</b> lifetime (ms) since opening: beyond this the transaction is rolled back even if active, keeping a
     * client that "pings" before each idle timeout from holding a connection indefinitely.
     */
    public static final long DEFAULT_MAX_LIFETIME_MS = 600_000L; // 10 min
    /** Window (ms) during which the outcome of a finished transaction is remembered, to make commit/rollback idempotent. */
    public static final long DEFAULT_OUTCOME_RETENTION_MS = 300_000L; // 5 min
    /** Global cap on open remote transactions (each one holds a connection) — protects the pool. */
    public static final int DEFAULT_MAX_OPEN = 128;
    /** Cap on open remote transactions per owner — keeps one client from monopolizing the pool. */
    public static final int DEFAULT_MAX_OPEN_PER_OWNER = 16;

    private final Supplier<DataSource> dataSourceSupplier;
    private final long idleTimeoutMs;
    private final long maxLifetimeMs;
    private final long outcomeRetentionMs;
    private final int maxOpen;
    private final int maxOpenPerOwner;
    private final ConcurrentHashMap<String, Entry> registry = new ConcurrentHashMap<>();
    /** Index owner → number of open transactions, for {@link #hasOpenTransactionForOwner} in O(1) (null owners are left out). */
    private final ConcurrentHashMap<String, Integer> openByOwner = new ConcurrentHashMap<>();
    /** Outcome of finished transactions (txId → committed/rolledback), retained for {@code outcomeRetentionMs} for idempotency. */
    private final ConcurrentHashMap<String, Finalized> outcomes = new ConcurrentHashMap<>();

    private static final Log LOG = Log.getLogger(RemoteTransactionCoordinatorImpl.class.getSimpleName());

    // Observability counters (cumulative totals since startup).
    private final AtomicLong begunCount = new AtomicLong();
    private final AtomicLong committedCount = new AtomicLong();
    private final AtomicLong rolledBackCount = new AtomicLong();
    private final AtomicLong reapedCount = new AtomicLong();
    private final AtomicLong rejectedByLimitCount = new AtomicLong();

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier) {
        this(dataSourceSupplier, RemoteTransactionOptions.defaults());
    }

    public RemoteTransactionCoordinatorImpl(Supplier<DataSource> dataSourceSupplier, RemoteTransactionOptions options) {
        this.dataSourceSupplier = dataSourceSupplier;
        this.idleTimeoutMs = options.idleTimeoutMs();
        this.maxLifetimeMs = options.maxLifetimeMs();
        this.outcomeRetentionMs = options.outcomeRetentionMs();
        this.maxOpen = options.maxOpen();
        this.maxOpenPerOwner = options.maxOpenPerOwner();
    }

    @Override
    public String begin(String ownerKey) {
        reapExpired(); // reclaims expired transactions before checking the caps
        enforceLimits(ownerKey);
        try {
            var scope = TransactionScope.beginNew(dataSource()); // CURRENT = scope (owner)
            TransactionScope.suspend(); // detaches from the thread; the scope stays alive
            var txId = UUID.randomUUID().toString();
            registry.put(txId, new Entry(scope, ownerKey));
            trackOwner(ownerKey);
            begunCount.incrementAndGet();
            LOG.debug("remote tx opened: {} (owner={}, open={})", txId, ownerKey, registry.size());
            return txId;
        } catch (Exception e) {
            throw new TransactionSystemException("Failed to open the remote transaction", e);
        }
    }

    @Override
    public void resume(String txId, String ownerKey) {
        var entry = require(txId);
        verifyOwner(entry.owner, ownerKey);
        if (!entry.inUse.compareAndSet(false, true)) {
            throw new TransactionSystemException("Remote transaction in concurrent use: " + txId);
        }
        try {
            TransactionScope.resume(entry.scope);
            entry.touch();
        } catch (RuntimeException e) {
            entry.inUse.set(false);
            throw e;
        }
    }

    @Override
    public void suspend(String txId) {
        var entry = registry.get(txId);
        if (entry == null) {
            return;
        }
        try {
            TransactionScope.suspend();
        } finally {
            entry.touch();
            entry.inUse.set(false);
        }
    }

    @Override
    public void commit(String txId, String ownerKey) {
        finish(txId, ownerKey, false);
    }

    @Override
    public void rollback(String txId, String ownerKey) {
        finish(txId, ownerKey, true);
    }

    @Override
    public String status(String txId, String ownerKey) {
        var entry = registry.get(txId);
        if (entry != null) {
            verifyOwner(entry.owner, ownerKey);
            return "open";
        }
        var prior = outcomes.get(txId);
        if (prior != null) {
            verifyOwner(prior.owner, ownerKey);
            return prior.committed ? "committed" : "rolledback";
        }
        return "unknown";
    }

    @Override
    public boolean exists(String txId) {
        return txId != null && registry.containsKey(txId);
    }

    @Override
    public boolean hasOpenTransactionForOwner(String ownerKey) {
        return ownerKey != null && openByOwner.containsKey(ownerKey);
    }

    @Override
    public RemoteTransactionStats stats() {
        return new RemoteTransactionStats(registry.size(), openByOwner.size(), outcomes.size(), begunCount.get(),
                committedCount.get(), rolledBackCount.get(), reapedCount.get(), rejectedByLimitCount.get());
    }

    // -------------------------------------------------------------------------

    /**
     * Rejects the opening if a cap is reached. A <b>soft</b> bound: under high concurrency it may be slightly exceeded
     * (non-atomic check), acceptable for protecting the pool — the goal is to limit, not to count exactly.
     */
    private void enforceLimits(String ownerKey) {
        if (registry.size() >= maxOpen) {
            rejectedByLimitCount.incrementAndGet();
            LOG.warn("GLOBAL cap on remote transactions reached ({}) — opening rejected", maxOpen);
            throw new TransactionLimitExceededException(
                    "Global limit of open remote transactions reached (" + maxOpen + ")");
        }
        if (ownerKey != null && openByOwner.getOrDefault(ownerKey, 0) >= maxOpenPerOwner) {
            rejectedByLimitCount.incrementAndGet();
            LOG.warn("PER-OWNER cap on remote transactions reached ({}) — owner={}", maxOpenPerOwner, ownerKey);
            throw new TransactionLimitExceededException(
                    "Per-owner limit of open remote transactions reached (" + maxOpenPerOwner + ")");
        }
    }

    /** Records +1 open transaction for the owner (ignores null owners). */
    private void trackOwner(String owner) {
        if (owner != null) {
            openByOwner.merge(owner, 1, Integer::sum);
        }
    }

    /** Records -1 transaction for the owner, removing the entry when it reaches zero (ignores null owners). */
    private void untrackOwner(String owner) {
        if (owner != null) {
            openByOwner.computeIfPresent(owner, (k, count) -> count <= 1 ? null : count - 1);
        }
    }

    private void finish(String txId, String ownerKey, boolean rollback) {
        var entry = registry.get(txId);
        if (entry != null) {
            verifyOwner(entry.owner, ownerKey);
            if (registry.remove(txId, entry)) {
                untrackOwner(entry.owner);
                closeAndRecord(txId, entry, rollback);
                return;
            }
            // another thread finished it between the get and the remove → treat as a retry
        }
        finishAlreadyFinalized(txId, ownerKey, rollback);
    }

    /** Closes the scope (commit/rollback) and records the outcome for idempotency. */
    private void closeAndRecord(String txId, Entry entry, boolean rollback) {
        try {
            if (rollback) {
                entry.scope.setRollbackOnly();
            }
            TransactionScope.resume(entry.scope);
            entry.scope.close(); // owner: commit (or rollback if marked) and close the connection
        } catch (Exception e) {
            throw new TransactionSystemException("Failed to finish the remote transaction " + txId, e);
        }
        outcomes.put(txId, new Finalized(!rollback, entry.owner));
        (rollback ? rolledBackCount : committedCount).incrementAndGet();
        AfterCommitTasks.runAll(entry.scope.takeAfterCommit());
        LOG.debug("remote tx finished ({}): {} (open={})", rollback ? "rollback" : "commit", txId,
                registry.size());
    }

    /** Retry path: the transaction is no longer open. Idempotent if the previous outcome matches the request. */
    private void finishAlreadyFinalized(String txId, String ownerKey, boolean rollback) {
        var prior = outcomes.get(txId);
        if (prior == null) {
            throw new UnknownTransactionException("Unknown or expired remote transaction: " + txId);
        }
        verifyOwner(prior.owner, ownerKey);
        if (prior.committed != !rollback) {
            throw new TransactionConflictException("Transaction " + txId + " already finished as "
                    + (prior.committed ? "committed" : "rolledback") + "; the opposite outcome is not accepted");
        }
        // same outcome already applied → successful no-op (previous response lost)
    }

    private void reapExpired() {
        var now = System.currentTimeMillis();
        if (!registry.isEmpty()) {
            var idleDeadline = now - idleTimeoutMs;
            for (var e : registry.entrySet()) {
                var entry = e.getValue();
                var idleExpired = entry.lastUsedAt < idleDeadline;
                var lifetimeExpired = (now - entry.createdAt) >= maxLifetimeMs;
                if ((idleExpired || lifetimeExpired) && !entry.inUse.get() && registry.remove(e.getKey(), entry)) {
                    untrackOwner(entry.owner);
                    rollbackQuietly(entry);
                    outcomes.put(e.getKey(), new Finalized(false, entry.owner)); // reaped = rolledback (disambiguates the retry)
                    reapedCount.incrementAndGet();
                    LOG.info("abandoned remote tx rolled back ({}): {} owner={}",
                            lifetimeExpired ? "lifetime" : "idle", e.getKey(), entry.owner);
                }
            }
        }
        if (!outcomes.isEmpty()) {
            var outcomeDeadline = now - outcomeRetentionMs;
            outcomes.entrySet().removeIf(en -> en.getValue().at < outcomeDeadline);
        }
    }

    private static void rollbackQuietly(Entry entry) {
        try {
            entry.scope.setRollbackOnly();
            TransactionScope.resume(entry.scope);
            entry.scope.close();
        } catch (Exception ignore) {
            // best effort — abandoned transaction
        }
    }

    private Entry require(String txId) {
        var entry = registry.get(txId);
        if (entry == null) {
            throw new UnknownTransactionException("Unknown or expired remote transaction: " + txId);
        }
        return entry;
    }

    /** If the transaction has an owner (non-null key at begin), requires the operation to present the same key. */
    private static void verifyOwner(String owner, String requesterKey) {
        if (owner != null && !owner.equals(requesterKey)) {
            throw new AccessDeniedException("Remote transaction does not belong to the requester");
        }
    }

    private DataSource dataSource() {
        var ds = dataSourceSupplier != null ? dataSourceSupplier.get() : null;
        if (ds == null) {
            throw new TransactionSystemException("DataSource not initialized for the remote coordinator");
        }
        return ds;
    }

    private static final class Entry {
        final TransactionScope scope;
        /** Opaque owner key (e.g. user id). {@code null} = transaction with no owner (security disabled). */
        final String owner;
        final AtomicBoolean inUse = new AtomicBoolean(false);
        final long createdAt = System.currentTimeMillis();
        volatile long lastUsedAt = System.currentTimeMillis();

        Entry(TransactionScope scope, String owner) {
            this.scope = scope;
            this.owner = owner;
        }

        void touch() {
            this.lastUsedAt = System.currentTimeMillis();
        }
    }

    /** Retained outcome of a finished transaction, to make commit/rollback idempotent. */
    private static final class Finalized {
        final boolean committed;
        final String owner;
        final long at = System.currentTimeMillis();

        Finalized(boolean committed, String owner) {
            this.committed = committed;
            this.owner = owner;
        }
    }
}
