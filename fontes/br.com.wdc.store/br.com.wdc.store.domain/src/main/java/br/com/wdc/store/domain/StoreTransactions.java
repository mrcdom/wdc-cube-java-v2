package br.com.wdc.store.domain;

import java.util.concurrent.atomic.AtomicReference;

import br.com.wdc.framework.domain.transaction.TransactionService;

/**
 * Per-module SPI: holder of the WDC Store application's {@link TransactionService}.
 *
 * <p>
 * Populated by the backend (composition root) with an instance bound to <b>this</b> module's {@code DataSource}. That
 * way each module has its own transaction service — and whether the backend shares or dedicates resources is
 * transparent to the module, which just uses {@code StoreTransactions.BEAN.get()}.
 * </p>
 */
public final class StoreTransactions {

    public static final AtomicReference<TransactionService> BEAN = new AtomicReference<>();

    private StoreTransactions() {
        // NOOP
    }
}
