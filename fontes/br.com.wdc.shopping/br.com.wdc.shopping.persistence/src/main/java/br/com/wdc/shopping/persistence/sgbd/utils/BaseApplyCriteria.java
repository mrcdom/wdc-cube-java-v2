package br.com.wdc.shopping.persistence.sgbd.utils;

import br.com.wdc.shopping.persistence.sgbd.dsl.SqlKeywords;
import br.com.wdc.shopping.persistence.sgbd.dsl.SqlList;

public abstract class BaseApplyCriteria implements SqlKeywords {

    private final BaseCommand cmd;

    protected BaseApplyCriteria(BaseCommand cmd) {
        this.cmd = cmd;
    }

    protected String param(String name, Object value) {
        return cmd.param(name, value);
    }

    public abstract void apply(SqlList sql);
}
