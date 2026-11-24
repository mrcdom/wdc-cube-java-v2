package br.com.wdc.framework.jooq;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.Operator;

/**
 * Translates a criteria field into a jOOQ condition.
 *
 * <p>
 * Written once, here, instead of repeated per entity: a change in the way of filtering is not multiplied across every
 * {@code ApplyConditions}, and adding a new operator means touching only this file.
 * </p>
 *
 * <p>
 * What stays per entity is only what it knows and the framework does not: which column matches each field and, when
 * the domain type differs from the column's, how to convert the value — with the types checked at compile time.
 * </p>
 *
 * <p>
 * <b>A field that is not set returns {@code null}</b> and stays out of the query; an entirely empty criteria results in
 * {@code noCondition()} — never a false condition, which would turn "no filter" into "no results".
 * </p>
 */
public final class CriterionTranslator {

    private CriterionTranslator() {
        // NOOP
    }

    /** Condition of a field whose value goes to the database as is. */
    public static <V> Condition translate(Field<V> column, Criterion<?, V> criterion) {
        return translate(column, criterion, null);
    }

    /**
     * Condition of a field whose value must be converted before reaching the column.
     *
     * @param <A> type of the value in the domain — the enum, {@code OffsetDateTime}, {@code Double}
     * @param <B> type of the column — {@code String}, {@code LocalDateTime}, {@code BigDecimal}
     */
    public static <A, B> Condition translate(Field<B> column, Criterion<?, A> criterion, Function<A, B> converter) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        var ofField = new ArrayList<Condition>();
        for (var predicate : criterion.predicates()) {
            var c = condition(column, predicate, converter);
            if (c != null) {
                ofField.add(c);
            }
        }
        if (ofField.isEmpty()) {
            return null;
        }
        if (ofField.size() == 1) {
            return ofField.get(0);
        }
        // AND by default — that is what makes ge(start) with le(end) mean a range, and ne(1) with ne(2) exclude both.
        // It becomes OR only when the field was marked with or(), and the disjunction stays contained in the field.
        return criterion.disjunctive() ? DSL.or(ofField) : DSL.and(ofField);
    }

    /**
     * Condition of a field over the given table, not over the one that declared the column.
     *
     * <p>
     * Serves the correlated subselect, where the table instance has its own alias. The column goes in as a
     * <b>name</b>: the field that goes into the query is the one resolved in {@code t}, already qualified by its alias.
     * </p>
     */
    public static <V> Condition translate(Table<?> t, TableField<?, V> column, Criterion<?, V> criterion) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        return translate(column(t, column), criterion, null);
    }

    /** Like {@link #translate(Table, TableField, Criterion)}, for a field that needs conversion. */
    public static <A, B> Condition translate(Table<?> t, TableField<?, B> column, Criterion<?, A> criterion,
            Function<A, B> converter) {
        if (criterion == null || !criterion.isSet()) {
            return null;
        }
        return translate(column(t, column), criterion, converter);
    }

    /**
     * The column, resolved in {@code t}.
     *
     * <p>
     * <b>Refuses when it does not exist, instead of ignoring the filter.</b> Returning {@code noCondition()} there would
     * deliver the whole set as if the filter had been applied — a wrong answer, and a silent one.
     * </p>
     */
    public static <V> Field<V> column(Table<?> t, TableField<?, V> column) {
        var field = t.field(column);
        if (field == null) {
            throw new IllegalArgumentException(
                    "column " + column.getName() + " does not exist in " + t.getName());
        }
        return field;
    }

    /** Joins the remaining conditions; an empty list returns {@code noCondition()}. */
    public static Condition and(List<Condition> conditions) {
        var useful = new ArrayList<Condition>();
        for (var c : conditions) {
            if (c != null) {
                useful.add(c);
            }
        }
        return useful.isEmpty() ? DSL.noCondition() : DSL.and(useful);
    }

    private static <A, B> Condition condition(Field<B> column, Criterion.Predicate<A> predicate,
            Function<A, B> converter) {

        var op = predicate.operator();

        if (op == Operator.IS_NULL) {
            return column.isNull();
        }
        if (op == Operator.IS_NOT_NULL) {
            return column.isNotNull();
        }

        var values = convert(predicate.values(), converter);
        if (!op.accepts(values.size())) {
            // Operator and values disagree — ignoring is better than emitting SQL the database would refuse.
            return null;
        }

        return switch (op) {
        case EQ -> column.eq(values.get(0));
        case NE -> column.ne(values.get(0));
        case GT -> column.gt(values.get(0));
        case GE -> column.ge(values.get(0));
        case LT -> column.lt(values.get(0));
        case LE -> column.le(values.get(0));
        case LIKE -> textual(column).like((String) values.get(0));
        case ILIKE -> textual(column).likeIgnoreCase((String) values.get(0));
        case BETWEEN -> column.between(values.get(0), values.get(1));
        case IN -> column.in(values);
        default -> null;
        };
    }

    /**
     * {@code like} and {@code ilike} are offered only on {@code TextCriterion}, which whoever declares the field picks by
     * the column type — so this cast is reached only for a textual column.
     */
    @SuppressWarnings("unchecked")
    private static <B> Field<String> textual(Field<B> column) {
        return (Field<String>) column;
    }

    private static <A, B> List<B> convert(Collection<A> values, Function<A, B> converter) {
        var result = new ArrayList<B>(values.size());
        for (var v : values) {
            @SuppressWarnings("unchecked")
            var converted = converter == null ? (B) v : converter.apply(v);
            result.add(converted);
        }
        return result;
    }
}
