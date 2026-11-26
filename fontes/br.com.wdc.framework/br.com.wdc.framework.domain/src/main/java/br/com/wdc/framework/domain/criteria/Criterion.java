package br.com.wdc.framework.domain.criteria;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Criterion on a field: the comparison requests made and how they combine.
 *
 * <p>
 * This class alone covers the field whose only sensible comparison is identity — enum, boolean, foreign key. Ordered
 * and text fields use the subclasses, which add what the type allows.
 * </p>
 *
 * <p>
 * It is the singular of {@code XxxCriteria} — a criteria aggregates several {@code Criterion}, one per filterable
 * field, and each field that is set becomes an {@code AND} condition in the query.
 * </p>
 *
 * <p>
 * <b>Successive requests accumulate, and by default they hold together ({@code AND}).</b> That is what makes a range
 * built in two lines — {@code ge(start)} then {@code le(end)}, as it comes out of two screen fields — mean what is
 * expected, and what makes {@code ne(1)} followed by {@code ne(2)} exclude both. Were {@code OR} the default, these two
 * cases would return nearly the whole table with nothing pointing out the error.
 * </p>
 *
 * <p>
 * <b>Alternatives are requested with {@link #or()}</b>, which marks the field as disjunctive:
 * </p>
 *
 * <pre>
 * criteria.name().or().startingWith("CAFE");
 * criteria.name().startingWith("CHA");
 * // (name ILIKE 'CAFE%' OR name ILIKE 'CHA%')
 * </pre>
 *
 * <p>
 * The disjunction applies <b>within</b> the field; across different fields the combination is always {@code AND}.
 * </p>
 *
 * <p>
 * <b>A null value adds no request</b>, instead of erasing the previous ones: this preserves the habit of building a
 * filter from screen fields, where empty means "do not filter by this". To erase what was already requested there is
 * {@link #clear()}; to compare with null, {@link #isNull()}.
 * </p>
 *
 * <p>
 * No method here knows jOOQ or SQL. The translation lives in {@code framework.jooq}, which matches {@link #name()} to
 * the column and converts the value; the REST client only serializes. This keeps the same class valid on both sides.
 * </p>
 *
 * @param <C> type of the {@code XxxCriteria} that holds this field
 * @param <T> type of the compared value
 */
public class Criterion<C, T> {

    /** A comparison request: the operator and the values that go with it. */
    public static final class Predicate<T> {

        private final Operator operator;
        private final List<T> values;

        Predicate(Operator operator, List<T> values) {
            this.operator = operator;
            this.values = values;
        }

        public Operator operator() {
            return operator;
        }

        public List<T> values() {
            return Collections.unmodifiableList(values);
        }

        /** First value, or {@code null} — shortcut for single-value operators. */
        public T value() {
            return values.isEmpty() ? null : values.get(0);
        }

        @Override
        public String toString() {
            return values.isEmpty() ? operator.toString() : operator + " " + values;
        }
    }

    /**
     * The {@code XxxCriteria} this field belongs to, so that requests can be chained.
     *
     * <p>
     * <b>It is not state, it is navigation</b> — and so it does not travel. Serializing this field would close a cycle
     * with the criteria that holds it. What describes the filter is the name, the requests and the disjunction; the
     * owner is re-established on the other side, when the criteria is rebuilt.
     * </p>
     */
    private transient C owner;

    private final String name;

    private final List<Predicate<T>> predicates = new ArrayList<>();

    private boolean disjunctive;

    public Criterion(C owner, String name) {
        this.owner = owner;
        this.name = name;
    }

    /**
     * Gives the owner back to a criterion that arrived without it — the case of a criterion rebuilt from the transport.
     *
     * <p>
     * Without an owner, {@code eq()} and friends would return {@code null} and the chain would break on first use.
     * </p>
     */
    public void rebind(C owner) {
        this.owner = owner;
    }

    /** {@code true} when the criterion does not yet know which {@code XxxCriteria} it belongs to. */
    public boolean detached() {
        return owner == null;
    }

    /** Logical name of the field — it is how the translation finds the matching column. */
    public String name() {
        return name;
    }

    /** Requests made on this field, in the order they were made. */
    public List<Predicate<T>> predicates() {
        return Collections.unmodifiableList(predicates);
    }

    /** {@code true} when the requests on this field combine with {@code OR}. */
    public boolean disjunctive() {
        return disjunctive;
    }

    /** {@code true} when there is at least one request, i.e. when this field must become a condition. */
    public boolean isSet() {
        return !predicates.isEmpty();
    }

    /**
     * Marks the field as disjunctive: its requests combine with {@code OR} among themselves.
     *
     * <p>
     * It applies to the whole field, not just to the next request — calling it once is enough, at any point.
     * </p>
     */
    public Criterion<C, T> or() {
        this.disjunctive = true;
        return this;
    }

    /** Requires all requests again ({@code AND}), undoing {@link #or()}. */
    public Criterion<C, T> and() {
        this.disjunctive = false;
        return this;
    }

    /** Erases the requests on this field, so it no longer filters. Does not undo {@link #or()}. */
    public C clear() {
        predicates.clear();
        return owner;
    }

    /** Equality. {@code null} adds no request. */
    public C eq(T value) {
        return value == null ? owner : add(Operator.EQ, value);
    }

    /** Not equal to. {@code null} adds no request. */
    public C ne(T value) {
        return value == null ? owner : add(Operator.NE, value);
    }

    /**
     * Belongs to the set. {@code null} adds no request; an empty set matches no row, so a search finds nothing, a count
     * is 0 and a delete deletes nothing.
     */
    public C in(Collection<? extends T> values) {
        if (values == null) {
            return owner;
        }
        predicates.add(new Predicate<>(Operator.IN, new ArrayList<>(values)));
        return owner;
    }

    /** Belongs to the set. */
    @SafeVarargs
    public final C in(T... values) {
        return in(values == null ? null : Arrays.asList(values));
    }

    public C isNull() {
        return add(Operator.IS_NULL);
    }

    public C isNotNull() {
        return add(Operator.IS_NOT_NULL);
    }

    /**
     * Restores a request exactly as it came from the transport, bypassing the rules of the fluent methods.
     *
     * <p>
     * It exists for deserialization, and only for it: {@code eq(null)} adds no request, which is right for whoever
     * builds the filter from a screen, but would silently erase an {@code EQ NULL} the other side sent.
     * </p>
     */
    public void restore(Operator operator, List<T> values) {
        predicates.add(new Predicate<>(operator, values == null ? new ArrayList<>() : new ArrayList<>(values)));
    }

    /** Sets the disjunction when rebuilding the criterion from the transport. */
    public void restoreDisjunctive(boolean disjunctive) {
        this.disjunctive = disjunctive;
    }

    /** Adds a request; visible to the subclasses, which offer the operators according to the field type. */
    @SafeVarargs
    protected final C add(Operator operator, T... values) {
        predicates.add(new Predicate<>(operator, new ArrayList<>(Arrays.asList(values))));
        return owner;
    }

    /** The owning criteria, for subclasses to return when chaining. */
    protected C owner() {
        return owner;
    }

    @Override
    public String toString() {
        if (!isSet()) {
            return name + " (not set)";
        }
        return name + " " + (disjunctive ? "any of " : "all of ") + predicates;
    }
}
