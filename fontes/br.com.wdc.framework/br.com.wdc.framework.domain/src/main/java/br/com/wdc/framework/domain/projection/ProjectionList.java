package br.com.wdc.framework.domain.projection;

import java.util.ArrayList;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * Projection collection of a 1:N relation — carries the projected bean, the child entity's criteria and, optionally, the
 * slice to apply.
 *
 * <p>
 * The criteria also carries the {@code OrderBy}, which is where the {@code ORDER BY} comes from. Without an order,
 * {@link #withLimit} and {@link #withOffset} cut rows in undefined order — see {@link HasSlice}.
 * </p>
 */
public class ProjectionList<E> extends ArrayList<E> implements HasCriteria, HasSlice {

    private static final long serialVersionUID = 8142609009973945555L;

    private transient Object criteria;

    private transient Integer limit;

    private transient Integer offset;

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
    public Integer getLimit() {
        return limit;
    }

    /** Maximum number of child rows to fetch. Order them too, or the cut is arbitrary. */
    public ProjectionList<E> withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public Integer getOffset() {
        return offset;
    }

    /** Child rows to skip before starting. Order them too, or the skip is arbitrary. */
    public ProjectionList<E> withOffset(Integer offset) {
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
