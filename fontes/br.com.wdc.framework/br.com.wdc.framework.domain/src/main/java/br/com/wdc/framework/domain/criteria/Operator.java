package br.com.wdc.framework.domain.criteria;

/**
 * Comparison operator of a {@link Criterion}, with the same semantics as SQL.
 *
 * <p>
 * It lives in {@code domain} and is deliberately <b>neutral</b>: it knows neither jOOQ nor SQL, it only names the
 * comparison. The translation to {@code Condition} belongs to {@code framework.jooq}; the REST client serializes the
 * same enum. Were this enum aware of jOOQ, the criteria could no longer be built on the client side.
 * </p>
 */
public enum Operator {

    /** {@code = ?} */
    EQ(1),
    /** {@code <> ?} */
    NE(1),
    /** {@code > ?} */
    GT(1),
    /** {@code >= ?} */
    GE(1),
    /** {@code < ?} */
    LT(1),
    /** {@code <= ?} */
    LE(1),
    /** {@code LIKE ?} — case-sensitive; the wildcards are part of the value. */
    LIKE(1),
    /** {@code ILIKE ?} — case-insensitive. */
    ILIKE(1),
    /** {@code BETWEEN ? AND ?} */
    BETWEEN(2),
    /** {@code IN (?, ?, …)} — variable arity. */
    IN(-1),
    /** {@code IS NULL} */
    IS_NULL(0),
    /** {@code IS NOT NULL} */
    IS_NOT_NULL(0);

    private final int arity;

    Operator(int arity) {
        this.arity = arity;
    }

    /** Number of values the operator requires; {@code -1} when variable ({@link #IN}). */
    public int arity() {
        return arity;
    }

    /** {@code true} when the given count satisfies the operator. */
    public boolean accepts(int count) {
        return arity < 0 ? count > 0 : count == arity;
    }
}
