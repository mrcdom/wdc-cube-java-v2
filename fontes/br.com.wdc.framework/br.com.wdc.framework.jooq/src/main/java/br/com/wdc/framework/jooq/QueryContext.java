package br.com.wdc.framework.jooq;

import org.jooq.DSLContext;

/**
 * Contexto de execução de uma query, gerencia alias únicos e acesso ao DSLContext.
 */
public class QueryContext {

    private final DSLContext jooq;
    private int uniqueInt;

    public QueryContext() {
        this.jooq = JooqDSLContext.BEAN.get();
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
