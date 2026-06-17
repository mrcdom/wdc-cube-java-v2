package br.com.wdc.shopping.persistence.impl.util;

import org.jooq.DSLContext;

import br.com.wdc.shopping.persistence.impl.ShoppingDSLContext;

public class BaseRepositoryImpl {
    
    protected static DSLContext dsl() {
        return ShoppingDSLContext.BEAN.get();
    }

}
