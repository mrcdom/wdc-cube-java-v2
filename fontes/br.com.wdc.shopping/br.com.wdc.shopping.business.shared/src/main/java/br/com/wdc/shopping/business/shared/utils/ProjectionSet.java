package br.com.wdc.shopping.business.shared.utils;

import java.util.HashSet;

public class ProjectionSet<E> extends HashSet<E> {

    private static final long serialVersionUID = 6928480631349456496L;

    private transient Object criteria;

    public ProjectionSet(E bean, Object criteria) {
        super(1);
        this.criteria = criteria;
        this.add(bean);
    }

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
