package br.com.wdc.shopping.domain.utils;

import java.util.HashSet;

import br.com.wdc.framework.commons.util.HasCriteria;

public class ProjectionSet<E> extends HashSet<E> implements HasCriteria {

    private static final long serialVersionUID = 6928480631349456496L;

    private transient Object criteria;

    public ProjectionSet(E bean, Object criteria) {
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
