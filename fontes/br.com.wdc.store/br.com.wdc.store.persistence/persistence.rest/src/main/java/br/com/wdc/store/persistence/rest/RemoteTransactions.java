package br.com.wdc.store.persistence.rest;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.persistence.transaction.RemoteTransactionCoordinator;

/**
 * <b>Server-side</b> holder of the {@link RemoteTransactionCoordinator} used by the REST layer.
 *
 * <p>
 * Unlike {@code StoreTransactions} (domain — holds the demarcation {@code TransactionService} the application uses),
 * this holder belongs to the <b>REST host</b>: the coordinator is a purely server-side persistence SPI (it keeps
 * physical transactions alive across HTTP requests) and must not be visible to the presentation layer. That is why it
 * lives here and not in the domain.
 * </p>
 *
 * <p>
 * Populated by the composition root (backend) with an instance bound to the module's {@code DataSource}; {@code null}
 * outside the server (e.g. local views, tests that do not exercise the coordinator).
 * </p>
 */
public final class RemoteTransactions {

    public static final AtomicReference<RemoteTransactionCoordinator> COORDINATOR = new AtomicReference<>();

    private RemoteTransactions() {
        // NOOP
    }
}
