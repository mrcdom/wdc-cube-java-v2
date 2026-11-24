package br.com.wdc.store.persistence.impl.util;

import org.jooq.DSLContext;

import br.com.wdc.store.persistence.impl.StoreDSLContext;

public class BaseRepositoryImpl {
    
    protected static DSLContext dsl() {
        return StoreDSLContext.BEAN.get();
    }

}
