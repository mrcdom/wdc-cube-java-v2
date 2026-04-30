package br.com.wdc.shopping.persistence.sgbd.utils;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.statement.SqlStatement;

import br.com.wdc.shopping.persistence.sgbd.dsl.SqlKeywords;

public class BaseCommand implements SqlKeywords {

    // :: Internal fields

    protected List<Pair<String, Object>> paramsList;

    protected String param(Object name, Object value) {
        if (paramsList == null) {
            paramsList = new ArrayList<>();
        }
        this.paramsList.add(Pair.of(String.valueOf(name), value));
        return ":" + name;
    }

    protected void applyParams(SqlStatement<?> stmt) {
        if (this.paramsList != null) {
            this.paramsList.forEach(arg -> stmt.bind(arg.getKey(), arg.getValue()));
        }
    }

    protected boolean paramsIsEmpty() {
        return paramsList == null || paramsList.isEmpty();
    }

}
