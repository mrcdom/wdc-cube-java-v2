package br.com.wdc.framework.domain.criteria;

/**
 * Criterion for an ordered field — number, date, timestamp.
 *
 * <p>
 * Adds to equality what only makes sense where an order exists. Splitting it into a subclass is what keeps
 * {@code criteria.roles().between(...)} from compiling: whoever declares the field knows the column type and picks the
 * class, so the restriction costs nothing to whoever writes the criteria and prevents the error at its source.
 * </p>
 *
 * @param <C> type of the {@code XxxCriteria} that holds this field
 * @param <T> type of the compared value
 */
public class ComparableCriterion<C, T> extends Criterion<C, T> {

    public ComparableCriterion(C owner, String name) {
        super(owner, name);
    }

    // Covariant return: without it, or() would return Criterion and the chain would lose between and the comparisons.
    @Override
    public ComparableCriterion<C, T> or() {
        super.or();
        return this;
    }

    @Override
    public ComparableCriterion<C, T> and() {
        super.and();
        return this;
    }

    /** Greater than. {@code null} adds no request. */
    public C gt(T value) {
        return value == null ? owner() : add(Operator.GT, value);
    }

    /** Greater than or equal to. {@code null} adds no request. */
    public C ge(T value) {
        return value == null ? owner() : add(Operator.GE, value);
    }

    /** Less than. {@code null} adds no request. */
    public C lt(T value) {
        return value == null ? owner() : add(Operator.LT, value);
    }

    /** Less than or equal to. {@code null} adds no request. */
    public C le(T value) {
        return value == null ? owner() : add(Operator.LE, value);
    }

    /**
     * Range closed at both ends.
     *
     * <p>
     * <b>Accepts an open bound.</b> Only the start becomes {@code >=}; only the end becomes {@code <=}; neither adds a
     * request. This is the everyday case of a period filter on screen, where usually only one date is filled in —
     * requiring both ends would push that decision onto every caller.
     * </p>
     */
    public C between(T start, T end) {
        if (start == null && end == null) {
            return owner();
        }
        if (start == null) {
            return le(end);
        }
        if (end == null) {
            return ge(start);
        }
        return add(Operator.BETWEEN, start, end);
    }
}
