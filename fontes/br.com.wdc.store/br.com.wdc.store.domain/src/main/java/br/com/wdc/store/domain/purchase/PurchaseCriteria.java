package br.com.wdc.store.domain.purchase;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;

/**
 * Search criteria for {@link Purchase}.
 *
 * <p>
 * {@code userId} and {@code productId} are not columns of this table: the first is the foreign key, the second exists
 * only in the items. The translation resolves each in its own way, but the criteria need not know that — whoever
 * builds it asks for what the purchase relates to, not for where the data is stored.
 * </p>
 */
public class PurchaseCriteria implements Criteria {

    // :: Projection

    private Purchase projection;

    public Purchase projection() {
        return projection;
    }

    public PurchaseCriteria withProjection(Purchase projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private final ComparableCriterion<PurchaseCriteria, Long> purchaseId = new ComparableCriterion<>(this, "purchaseId");

    public ComparableCriterion<PurchaseCriteria, Long> purchaseId() {
        return purchaseId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasPurchaseId() {
        return purchaseId.isSet();
    }

    /** Shortcut for {@code purchaseId().eq(value)}; {@code null} does not filter. */
    public PurchaseCriteria withPurchaseId(Long value) {
        return value == null ? this : purchaseId().eq(value);
    }

    private final ComparableCriterion<PurchaseCriteria, Long> userId = new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<PurchaseCriteria, Long> userId() {
        return userId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Shortcut for {@code userId().eq(value)}; {@code null} does not filter. */
    public PurchaseCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    /** The column stores no time zone: the value is converted to {@code LocalDateTime} before the comparison. */
    private final ComparableCriterion<PurchaseCriteria, OffsetDateTime> buyDate = new ComparableCriterion<>(this, "buyDate");

    public ComparableCriterion<PurchaseCriteria, OffsetDateTime> buyDate() {
        return buyDate;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasBuyDate() {
        return buyDate.isSet();
    }

    /** Shortcut for {@code buyDate().eq(value)}; {@code null} does not filter. */
    public PurchaseCriteria withBuyDate(OffsetDateTime value) {
        return value == null ? this : buyDate().eq(value);
    }

    /** Purchases that contain the product — resolved by {@code EXISTS} over the items. */
    private final ComparableCriterion<PurchaseCriteria, Long> productId = new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<PurchaseCriteria, Long> productId() {
        return productId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Shortcut for {@code productId().eq(value)}; {@code null} does not filter. */
    public PurchaseCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(purchaseId, userId, buyDate, productId);
    }

    // :: Alternative groups

    private List<PurchaseCriteria> anyOf = List.of();

    @Override
    public List<PurchaseCriteria> anyOf() {
        return anyOf;
    }

    /** Whether there are alternative groups. */
    public boolean hasAnyOf() {
        return !anyOf.isEmpty();
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public PurchaseCriteria withAnyOf(List<PurchaseCriteria> groups) {
        this.anyOf = groups == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(groups));
        return this;
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public PurchaseCriteria withAnyOf(PurchaseCriteria... groups) {
        return withAnyOf(Arrays.asList(groups));
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public PurchaseCriteria withOrderBy(OrderBy orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /**
     * Provisioned orderings.
     *
     * <p>
     * Each constant is a <b>complete ordering</b>, not a request to sort by a field: the name states the resulting
     * effect, and the translation — in the repository — decides which columns and directions produce it. There is no
     * composition; you pick one.
     * </p>
     *
     * <p>
     * <b>The list is short on purpose.</b> A new ordering is added on request, together with the index that supports
     * it — that keeps explicit what the database has to handle. Offering free ordering by any field would look generous
     * and would produce a full scan on the first large query.
     * </p>
     */
    public enum OrderBy {
        /** Recording order — the purchase recorded first comes first. */
        OLDEST_FIRST,
        /** Reverse recording order. */
        NEWEST_FIRST,
        /** By purchase date, most recent first — the usual order of a statement. */
        MOST_RECENT_PURCHASE_FIRST,
        /** By purchase date, oldest first. */
        EARLIEST_PURCHASE_FIRST,
    }

}
