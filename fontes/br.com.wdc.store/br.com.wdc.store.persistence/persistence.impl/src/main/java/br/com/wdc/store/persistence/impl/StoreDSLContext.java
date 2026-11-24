package br.com.wdc.store.persistence.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.jooq.DSLContext;

/**
 * Holder of the WDC Store application's {@link DSLContext} (jOOQ).
 *
 * <p>
 * It belongs to the application — not to the framework — so that multiple applications on the same backend can point
 * to distinct {@code DSLContext}s/databases without colliding in a shared global holder. {@code framework.jooq} receives
 * this context by injection (via {@code JsonQueryBuilder.setDSLContextSupplier}).
 * </p>
 */
public final class StoreDSLContext {

    public static final AtomicReference<DSLContext> BEAN = new AtomicReference<>();

    private StoreDSLContext() {
        // NOOP
    }
}
