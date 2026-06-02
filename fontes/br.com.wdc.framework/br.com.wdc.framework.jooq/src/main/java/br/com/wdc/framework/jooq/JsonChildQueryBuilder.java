package br.com.wdc.framework.jooq;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

import br.com.wdc.framework.commons.util.HasCriteria;

/**
 * Construtor de cláusula WHERE para queries filhas (relações 1:N).
 *
 * <p>
 * Disponibiliza acesso à tabela pai ({@code superTable}) e à tabela filha ({@code childTable}) para que o chamador
 * defina a condição de correlação.
 * </p>
 *
 * <p>
 * Quando o bean de projeção pai contém uma coleção com critério ({@link HasCriteria}), o critério embutido é
 * propagado via {@link #getCriteria()} / {@link #getCriteriaAs(Class)} para que o {@code childWhereClause} possa
 * aplicá-lo na query filha.
 * </p>
 *
 * @param <S> tipo da tabela-schema pai (ex: TUsuario)
 * @param <C> tipo da tabela-schema filha (ex: TConector)
 */
public class JsonChildQueryBuilder<S, C> {

    final QueryContext ctx;
    final S superTable;
    C childTable;
    Table<Record> childJooqTable;
    SelectJoinStep<Record1<String>> dsl;
    Object criteria;

    JsonChildQueryBuilder(QueryContext ctx, S superTable) {
        this.ctx = ctx;
        this.superTable = superTable;
    }
    
    public QueryContext getCtx() {
        return ctx;
    }

    public SelectJoinStep<Record1<String>> dsl() {
        return dsl;
    }

    public SelectConditionStep<Record1<String>> where() {
        return dsl.where();
    }

    public S getSuperTable() {
        return superTable;
    }

    public C getChildTable() {
        return childTable;
    }

    /**
     * Critério propagado a partir de uma coleção {@link HasCriteria}, ou {@code null} se nenhum critério foi fornecido.
     */
    public Object getCriteria() {
        return criteria;
    }

    /**
     * Retorna o critério convertido para o tipo esperado.
     *
     * @throws ClassCastException se o critério não for do tipo solicitado
     */
    public <X> X getCriteriaAs(Class<X> type) {
        return type.cast(criteria);
    }

    public String uniqueName(String name) {
        return ctx.alias(name);
    }
}
