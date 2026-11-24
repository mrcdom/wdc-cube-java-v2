package br.com.wdc.framework.domain.projection;

import java.util.HashSet;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * Projection collection of a 1:N relation — carries the projected bean, the child entity's criteria and, optionally, the
 * slice to apply.
 *
 * <p>
 * The criteria also carries the {@code OrderBy}, which is where the order comes from. <b>Asking for an order here is not
 * pointless</b>: the set read from the query is a {@code LinkedHashSet}, which preserves insertion order — it is this
 * class, the projection one, that holds a single element and needs no ordering.
 * </p>
 */
public class ProjectionSet<E> extends HashSet<E> implements HasCriteria, HasSlice {

    private static final long serialVersionUID = 6928480631349456496L;

    private transient Object criteria;

    private transient Integer limit;

    private transient Integer offset;

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
    public Integer getLimit() {
        return limit;
    }

    /** Maximum number of child rows to fetch. Order them too, or the cut is arbitrary. */
    public ProjectionSet<E> withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public Integer getOffset() {
        return offset;
    }

    /** Child rows to skip before starting. Order them too, or the skip is arbitrary. */
    public ProjectionSet<E> withOffset(Integer offset) {
        this.offset = offset;
        return this;
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
