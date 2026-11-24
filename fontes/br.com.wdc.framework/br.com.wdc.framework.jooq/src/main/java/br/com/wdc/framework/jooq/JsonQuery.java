package br.com.wdc.framework.jooq;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.Table;

import com.google.gson.stream.JsonReader;

/**
 * Built instance, ready for use.
 *
 * @param <B> bean type
 * @param <T> schema table type
 */
public interface JsonQuery<B, T extends Table<?>> {

    /** Creates an empty bean. */
    B newBean();

    /**
     * Creates a projection bean with ALL fields set (non-null sentinel). Passing this bean to {@link #fetchOne} /
     * {@link #fetchToList} selects every column.
     */
    B newProjectionBean();

    /**
     * Creates a full projection bean and lets the caller "null out" unwanted fields. Fields set to {@code null} in the
     * callback will NOT be included in the SELECT.
     */
    B newProjectionBean(Consumer<B> adapt);

    /** Creates a new schema table instance with an alias. */
    T newTable(String alias);

    /** Builds the SELECT query (without executing it). */
    SelectQuery<Record1<String>> select(B prjBean,
            BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Builds the SELECT query with a context and an aggregation flag (internal use). */
    SelectQuery<Record1<String>> select(QueryContext ctx, B prjBean,
            BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause, boolean isAgg);

    /** Whether this query knows how to translate the {@code OrderBy} of its criteria. */
    boolean hasOrdering();

    /** Translates the criteria's {@code OrderBy} into {@code ORDER BY}, against the given table. */
    List<SortField<?>> orderingOf(T table, Object criteria);

    /**
     * Builds the aggregated SELECT of a child collection, ordered and/or sliced.
     *
     * <p>
     * <b>Ordering and slicing do not fit the aggregated form.</b> The collection comes out of a
     * {@code SELECT json_arrayagg(proj) FROM child WHERE ...}; an {@code ORDER BY} there would apply to the query that
     * has already aggregated. The way out is to order and slice <b>before</b> aggregating, in a derived table.
     * </p>
     *
     * <p>
     * <b>The order is a function of the table</b>, not a ready-made list: the child table instance — with the unique
     * alias of that query — exists only in here.
     * </p>
     *
     * @param order  translates the criteria into {@code ORDER BY}; {@code null} leaves the order to the database
     * @param limit  maximum number of rows, or {@code null}
     * @param offset rows to skip, or {@code null}
     */
    SelectQuery<Record1<String>> selectOrdered(QueryContext ctx, B prjBean,
            BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause,
            Function<T, List<SortField<?>>> order, Integer limit, Integer offset);

    /** Generates the JSON projection field for the table. */
    Field<String> projection(QueryContext ctx, T jooqTable, B prjBean);

    /**
     * Whether the projection asks for any field outside the given list.
     *
     * <p>
     * Serves the foreign key shortcut: when the association's projection asks for nothing beyond the key, the value is
     * already in the current row's column, and the subselect to the other side can be skipped.
     * </p>
     */
    boolean projectsBeyond(B prjBean, Collection<String> fields);

    /** Parses a JSON string into a bean. */
    B parseJson(String json);

    /** Parses JSON from a reader into a bean. */
    B parseJson(JsonReader jsonReader);

    /** Executes and returns a single bean (or null). */
    B fetchOne(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Executes and returns a (lazy) stream. */
    Stream<B> fetch(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Executes and returns a materialized list. */
    List<B> fetchToList(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause);

    /** Counts records. */
    int fetchCount(BiConsumer<T, SelectJoinStep<Record1<Integer>>> whereClause);
}
