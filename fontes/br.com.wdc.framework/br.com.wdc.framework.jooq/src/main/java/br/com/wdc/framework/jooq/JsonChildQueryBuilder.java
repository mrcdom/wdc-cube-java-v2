package br.com.wdc.framework.jooq;

import org.jooq.Record;
import org.jooq.Record1;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.Table;

import br.com.wdc.framework.commons.util.HasCriteria;

/**
 * WHERE clause builder for child queries (1:N relations).
 *
 * <p>
 * Gives access to the parent table ({@code superTable}) and to the child table ({@code childTable}) so that the caller
 * defines the correlation condition.
 * </p>
 *
 * <p>
 * When the parent projection bean holds a collection with criteria ({@link HasCriteria}), the embedded criteria is
 * propagated via {@link #getCriteria()} / {@link #getCriteriaAs(Class)} so that the {@code childWhereClause} can
 * apply it to the child query.
 * </p>
 *
 * @param <S> type of the parent schema table (e.g. TUser)
 * @param <C> type of the child schema table (e.g. TConnector)
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
     * Criteria propagated from a {@link HasCriteria} collection, or {@code null} if no criteria was supplied.
     */
    public Object getCriteria() {
        return criteria;
    }

    /**
     * Returns the criteria cast to the expected type.
     *
     * @throws ClassCastException if the criteria is not of the requested type
     */
    public <X> X getCriteriaAs(Class<X> type) {
        return type.cast(criteria);
    }

    public String uniqueName(String name) {
        return ctx.alias(name);
    }
}
