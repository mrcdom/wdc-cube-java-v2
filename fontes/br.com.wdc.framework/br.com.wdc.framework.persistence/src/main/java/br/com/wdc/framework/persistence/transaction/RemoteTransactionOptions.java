package br.com.wdc.framework.persistence.transaction;

import br.com.wdc.framework.domain.config.AppConfig;

/**
 * Operational parameters of the {@link RemoteTransactionCoordinatorImpl} — externalizable via configuration. Immutable.
 * Missing values fall back to the defaults defined in {@link RemoteTransactionCoordinatorImpl} ({@code DEFAULT_*}).
 */
public record RemoteTransactionOptions(
        long idleTimeoutMs,
        long maxLifetimeMs,
        long outcomeRetentionMs,
        int maxOpen,
        int maxOpenPerOwner) {

    /** All parameters at the coordinator's defaults. */
    public static RemoteTransactionOptions defaults() {
        return new RemoteTransactionOptions(
                RemoteTransactionCoordinatorImpl.DEFAULT_IDLE_TIMEOUT_MS,
                RemoteTransactionCoordinatorImpl.DEFAULT_MAX_LIFETIME_MS,
                RemoteTransactionCoordinatorImpl.DEFAULT_OUTCOME_RETENTION_MS,
                RemoteTransactionCoordinatorImpl.DEFAULT_MAX_OPEN,
                RemoteTransactionCoordinatorImpl.DEFAULT_MAX_OPEN_PER_OWNER);
    }

    /**
     * Reads from {@code application.toml} under {@code <prefix>database.remoteTransaction.*}; durations in <b>seconds</b>
     * (project convention, like {@code database.pool.connectionTimeoutSeconds}). Missing key → the impl's default.
     */
    public static RemoteTransactionOptions fromConfig(AppConfig config, String prefix) {
        var d = defaults();
        var base = prefix + "database.remoteTransaction.";
        return new RemoteTransactionOptions(
                config.getInt(base + "idleTimeoutSeconds", (int) (d.idleTimeoutMs / 1000)) * 1000L,
                config.getInt(base + "maxLifetimeSeconds", (int) (d.maxLifetimeMs / 1000)) * 1000L,
                config.getInt(base + "outcomeRetentionSeconds", (int) (d.outcomeRetentionMs / 1000)) * 1000L,
                config.getInt(base + "maxOpen", d.maxOpen),
                config.getInt(base + "maxOpenPerOwner", d.maxOpenPerOwner));
    }

    public RemoteTransactionOptions withIdleTimeoutMs(long v) {
        return new RemoteTransactionOptions(v, maxLifetimeMs, outcomeRetentionMs, maxOpen, maxOpenPerOwner);
    }

    public RemoteTransactionOptions withMaxLifetimeMs(long v) {
        return new RemoteTransactionOptions(idleTimeoutMs, v, outcomeRetentionMs, maxOpen, maxOpenPerOwner);
    }

    public RemoteTransactionOptions withMaxOpen(int v) {
        return new RemoteTransactionOptions(idleTimeoutMs, maxLifetimeMs, outcomeRetentionMs, v, maxOpenPerOwner);
    }

    public RemoteTransactionOptions withMaxOpenPerOwner(int v) {
        return new RemoteTransactionOptions(idleTimeoutMs, maxLifetimeMs, outcomeRetentionMs, maxOpen, v);
    }
}
