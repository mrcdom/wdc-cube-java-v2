package br.com.wdc.framework.domain.projection;

import java.util.HashSet;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * Coleção de projeção de uma relação 1:N — carrega o bean projetado, o critério da entidade filha e, opcionalmente, o
 * recorte a aplicar.
 *
 * <p>
 * O critério traz também o {@code OrderBy}, e é dele que sai a ordem. <b>Pedir ordem aqui não é inútil</b>: o conjunto
 * lido da consulta é um {@code LinkedHashSet}, que preserva a ordem de inserção — é esta classe, a da projeção, que
 * guarda um elemento só e não precisa ordenar nada.
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

    /** Máximo de linhas filhas a trazer. Ordene também, ou o corte é arbitrário. */
    public ProjectionSet<E> withLimit(Integer limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public Integer getOffset() {
        return offset;
    }

    /** Linhas filhas a pular antes de começar. Ordene também, ou o salto é arbitrário. */
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
