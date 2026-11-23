package br.com.wdc.framework.persistence.transaction;

/**
 * Observability snapshot of the {@link RemoteTransactionCoordinator}: instantaneous <b>gauges</b> (current state) and
 * cumulative <b>counters</b> (totals since startup). Immutable — suitable for exposing on a metrics endpoint.
 *
 * @param openNow         remote transactions open at this instant (each one holds a connection)
 * @param ownersWithOpen  distinct owners with at least one open transaction
 * @param retainedOutcomes finished outcomes still retained (for idempotency)
 * @param begun           total transactions opened
 * @param committed       total committed
 * @param rolledBack      total rolled back (by explicit commit-rollback)
 * @param reaped          total rolled back by the reaper (idle/lifetime — abandonment)
 * @param rejectedByLimit total openings rejected by a cap (global or per owner)
 */
public record RemoteTransactionStats(
        int openNow,
        int ownersWithOpen,
        int retainedOutcomes,
        long begun,
        long committed,
        long rolledBack,
        long reaped,
        long rejectedByLimit) {
}
