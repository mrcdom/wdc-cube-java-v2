package br.com.wdc.framework.jooq;

import org.jooq.DSLContext;

/**
 * Execution context of a query: manages unique aliases and gives access to the {@link DSLContext}.
 * <p>
 * The {@code DSLContext} is <b>injected</b> (not read from a global holder), so that each application supplies its own — see
 * {@code JsonQueryBuilder.setDSLContextSupplier}.
 * </p>
 */
public class QueryContext {

    private final DSLContext jooq;
    private int uniqueInt;

    public QueryContext(DSLContext jooq) {
        this.jooq = jooq;
    }

    public String alias(String name) {
        return name + nextUniqueInt();
    }

    public int nextUniqueInt() {
        return ++uniqueInt;
    }

    public DSLContext dsl() {
        return jooq;
    }
}
