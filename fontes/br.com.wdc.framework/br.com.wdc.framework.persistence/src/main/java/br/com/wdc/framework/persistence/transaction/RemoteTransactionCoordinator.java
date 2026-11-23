package br.com.wdc.framework.persistence.transaction;

/**
 * Coordinator of <b>client-driven transactions over REST</b> (server side).
 *
 * <p>
 * Lets the client demarcate a transaction that spans <b>several HTTP requests</b> against the same backend: the server
 * keeps the physical transaction open between calls, identified by a {@code txId}, and reattaches it (resume) to the
 * thread serving each request.
 * </p>
 *
 * <ul>
 * <li>{@link #begin(String)} — opens a transaction, suspends it and returns its {@code txId};</li>
 * <li>{@link #resume(String, String)} / {@link #suspend(String)} — attaches/detaches the transaction on the current
 * thread (around each request that carries the {@code txId});</li>
 * <li>{@link #commit(String, String)} / {@link #rollback(String, String)} — finish it and remove it from the
 * registry.</li>
 * </ul>
 *
 * <p>
 * <b>Owner (ownerKey):</b> an <b>opaque</b> key given in {@link #begin(String)} and revalidated in every following
 * operation — the coordinator only compares strings, with no notion of a user. If {@code begin} received a non-null
 * key, any operation with a different key is rejected. Translating identity → key (and what to do with {@code null})
 * is up to the calling layer.
 * </p>
 *
 * <p>
 * A <b>purely server-side persistence</b> SPI: implemented in this module (bound to the module's {@code DataSource})
 * and consumed only by the REST host. It is not visible to the presentation layer — that is why it lives in
 * {@code framework.persistence}, not in {@code framework.domain}. Each {@code txId} keeps a connection/transaction alive
 * until commit/rollback — that is why there is a cleanup timeout for abandoned transactions.
 * </p>
 */
public interface RemoteTransactionCoordinator {

    /**
     * Opens a (suspended) transaction and returns its opaque identifier.
     *
     * @param ownerKey opaque owner key (revalidated in the following operations); {@code null} = no owner
     */
    String begin(String ownerKey);

    /** Reattaches the transaction to the current thread. Fails if unknown, already in use, or if {@code ownerKey} differs. */
    void resume(String txId, String ownerKey);

    /** Detaches the transaction from the current thread, keeping it alive for the next request. */
    void suspend(String txId);

    /**
     * Finishes with COMMIT and removes it from the registry (validates {@code ownerKey}). <b>Idempotent</b>: repeating
     * the commit of an already committed transaction (previous response lost) is a successful no-op, within the outcome
     * retention window. Repeating as a rollback something already committed conflicts
     * ({@code TransactionConflictException}).
     */
    void commit(String txId, String ownerKey);

    /**
     * Finishes with ROLLBACK and removes it from the registry (validates {@code ownerKey}). <b>Idempotent</b>, symmetric to
     * {@link #commit(String, String)}.
     */
    void rollback(String txId, String ownerKey);

    /**
     * Transaction state for the requester, disambiguating a lost finish response.
     *
     * @return {@code "open"} (registered and alive), {@code "committed"}/{@code "rolledback"} (finished, within
     *         retention) or {@code "unknown"} (never existed or retention expired). Validates {@code ownerKey}.
     */
    String status(String txId, String ownerKey);

    /** @return {@code true} if there is a registered transaction for {@code txId}. */
    boolean exists(String txId);

    /** @return metrics snapshot (current gauges + cumulative counters) for observability. */
    RemoteTransactionStats stats();

    /**
     * @return {@code true} if there is any open remote transaction for this (non-null) {@code ownerKey}. Lets the host
     *         detect a write that arrived <b>without</b> the {@code txId} although the requester has an open remote
     *         transaction — a sign of broken header propagation, which would otherwise autocommit outside the
     *         transaction.
     */
    boolean hasOpenTransactionForOwner(String ownerKey);

}
