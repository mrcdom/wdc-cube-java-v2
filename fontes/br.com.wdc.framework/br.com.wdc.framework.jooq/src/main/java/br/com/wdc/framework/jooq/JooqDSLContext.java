package br.com.wdc.framework.jooq;

import java.util.concurrent.atomic.AtomicReference;

import org.jooq.DSLContext;

public class JooqDSLContext {

    private JooqDSLContext() {
        // NOOP
    }

    public static final AtomicReference<DSLContext> BEAN = new AtomicReference<>();

}
