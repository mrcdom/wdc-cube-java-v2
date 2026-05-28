package br.com.wdc.shopping.domain.utils;

import java.util.ArrayList;

import br.com.wdc.framework.commons.util.HasCriteria;

public class ProjectionList<E> extends ArrayList<E> implements HasCriteria {

    private static final long serialVersionUID = 8142609009973945555L;

    private transient Object criteria;

    public ProjectionList(E bean, Object criteria) {
        super(1);
        this.criteria = criteria;
        this.add(bean);
    }

    @Override
    public Object getCriteria() {
        return criteria;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

}
