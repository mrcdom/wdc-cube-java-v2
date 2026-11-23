package br.com.wdc.framework.domain.criteria;

/**
 * Criterion for a text field.
 *
 * <p>
 * Extends {@link ComparableCriterion} because text is ordered too — {@code name().ge("M")} is a legitimate query — and
 * adds pattern searches.
 * </p>
 *
 * <p>
 * <b>The wildcards are part of the value.</b> {@code like("%CAFE%")} differs from {@code like("CAFE")}, exactly as in
 * SQL. Adding them automatically would seem convenient, but would take from whoever writes the query the ability to
 * anchor at the start — which is precisely what decides whether an index is used.
 * </p>
 *
 * @param <C> type of the {@code XxxCriteria} that holds this field
 */
public class TextCriterion<C> extends ComparableCriterion<C, String> {

    public TextCriterion(C owner, String name) {
        super(owner, name);
    }

    // Covariant return: without it, or() would return the superclass and the chain would lose like and ilike.
    @Override
    public TextCriterion<C> or() {
        super.or();
        return this;
    }

    @Override
    public TextCriterion<C> and() {
        super.and();
        return this;
    }

    /** {@code LIKE}, case-sensitive. Null or empty text adds no request. */
    public C like(String pattern) {
        return empty(pattern) ? owner() : add(Operator.LIKE, pattern);
    }

    /** {@code ILIKE}, case-insensitive. Null or empty text adds no request. */
    public C ilike(String pattern) {
        return empty(pattern) ? owner() : add(Operator.ILIKE, pattern);
    }

    /** {@code ILIKE '%value%'} — the most common substring search, with the wildcards added for convenience. */
    public C containing(String fragment) {
        return empty(fragment) ? owner() : ilike("%" + fragment + "%");
    }

    /** {@code ILIKE 'value%'} — anchored at the start, which is what lets the database use an index. */
    public C startingWith(String prefix) {
        return empty(prefix) ? owner() : ilike(prefix + "%");
    }

    private static boolean empty(String s) {
        return s == null || s.isEmpty();
    }
}
