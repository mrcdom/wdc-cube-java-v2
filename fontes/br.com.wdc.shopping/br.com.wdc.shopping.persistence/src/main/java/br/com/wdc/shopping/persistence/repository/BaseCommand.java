package br.com.wdc.shopping.persistence.repository;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.jdbi.v3.core.statement.SqlStatement;

import br.com.wdc.shopping.persistence.sql.SqlKeywords;

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

    public void transferParamsTo(BaseCommand target) {
        if (this.paramsList != null) {
            this.paramsList.forEach(p -> target.param(p.getKey(), p.getValue()));
        }
    }

}
