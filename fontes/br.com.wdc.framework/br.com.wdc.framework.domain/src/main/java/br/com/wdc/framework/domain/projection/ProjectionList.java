package br.com.wdc.framework.domain.projection;

import java.util.ArrayList;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * Coleção de projeção de uma relação 1:N — carrega o bean projetado, o critério da entidade filha e, opcionalmente, o
 * recorte a aplicar.
 *
 * <p>
 * O critério traz também o {@code OrderBy}, e é dele que sai a ordem do {@code ORDER BY}. Sem ordem, {@link #withLimit}
 * e {@link #withOffset} cortam linhas em ordem indefinida — ver {@link HasSlice}.
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

    /** Máximo de linhas filhas a trazer. Ordene também, ou o corte é arbitrário. */
    public ProjectionList<E> withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public Integer getOffset() {
        return offset;
    }

    /** Linhas filhas a pular antes de começar. Ordene também, ou o salto é arbitrário. */
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
