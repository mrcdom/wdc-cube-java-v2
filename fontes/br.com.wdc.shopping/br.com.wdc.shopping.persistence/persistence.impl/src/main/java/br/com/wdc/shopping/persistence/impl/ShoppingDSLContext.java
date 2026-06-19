package br.com.wdc.shopping.persistence.impl;

import java.util.concurrent.atomic.AtomicReference;

import org.jooq.DSLContext;

/**
 * Holder do {@link DSLContext} (jOOQ) da aplicação Shopping.
 *
 * <p>
 * Pertence à aplicação — não ao framework — para que múltiplas aplicações no mesmo backend possam apontar para
 * {@code DSLContext}/bancos distintos sem colidir num holder global compartilhado. O {@code framework.jooq} recebe este
 * contexto por injeção (via {@code JsonQueryBuilder.setDSLContextSupplier}).
 * </p>
 */
public final class ShoppingDSLContext {

    public static final AtomicReference<DSLContext> BEAN = new AtomicReference<>();

    private ShoppingDSLContext() {
        // NOOP
    }
}
