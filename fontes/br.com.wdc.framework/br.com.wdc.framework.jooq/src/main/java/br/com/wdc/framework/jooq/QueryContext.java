package br.com.wdc.framework.jooq;

import org.jooq.DSLContext;

/**
 * Contexto de execução de uma query: gerencia alias únicos e dá acesso ao {@link DSLContext}.
 * <p>
 * O {@code DSLContext} é <b>injetado</b> (não lido de um holder global), de modo que cada aplicação forneça o seu — ver
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
