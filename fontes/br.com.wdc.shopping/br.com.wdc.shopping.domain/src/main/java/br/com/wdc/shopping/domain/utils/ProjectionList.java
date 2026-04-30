package br.com.wdc.shopping.domain.utils;

import java.util.ArrayList;

public class ProjectionList<E> extends ArrayList<E> {

    private static final long serialVersionUID = 8142609009973945555L;

    private transient Object criteria;

    public ProjectionList(E bean, Object criteria) {
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
