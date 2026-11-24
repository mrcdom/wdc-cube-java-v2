package br.com.wdc.store.domain.purchaseitem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;

/**
 * Search criteria for {@link PurchaseItem}.
 *
 * <p>
 * {@code userId} is not a column of this table — it belongs to the purchase owner. The translation resolves it with
 * {@code EXISTS} over {@code EN_PURCHASE}; for whoever builds the filter, it is just another field.
 * </p>
 */
public class PurchaseItemCriteria implements Criteria {

    // :: Projection

    private PurchaseItem projection;

    public PurchaseItem projection() {
        return projection;
    }

    public PurchaseItemCriteria withProjection(PurchaseItem projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId = new ComparableCriterion<>(this, "purchaseItemId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId() {
        return purchaseItemId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasPurchaseItemId() {
        return purchaseItemId.isSet();
    }

    /** Shortcut for {@code purchaseItemId().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withPurchaseItemId(Long value) {
        return value == null ? this : purchaseItemId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseId = new ComparableCriterion<>(this, "purchaseId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseId() {
        return purchaseId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasPurchaseId() {
        return purchaseId.isSet();
    }

    /** Shortcut for {@code purchaseId().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withPurchaseId(Long value) {
        return value == null ? this : purchaseId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> productId = new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<PurchaseItemCriteria, Long> productId() {
        return productId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Shortcut for {@code productId().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Integer> amount = new ComparableCriterion<>(this, "amount");

    public ComparableCriterion<PurchaseItemCriteria, Integer> amount() {
        return amount;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasAmount() {
        return amount.isSet();
    }

    /** Shortcut for {@code amount().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withAmount(Integer value) {
        return value == null ? this : amount().eq(value);
    }

    /** The value is converted to {@code BigDecimal} before it reaches the column. */
    private final ComparableCriterion<PurchaseItemCriteria, Double> price = new ComparableCriterion<>(this, "price");

    public ComparableCriterion<PurchaseItemCriteria, Double> price() {
        return price;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasPrice() {
        return price.isSet();
    }

    /** Shortcut for {@code price().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withPrice(Double value) {
        return value == null ? this : price().eq(value);
    }

    /** Items of the user's purchases — resolved by {@code EXISTS} over the purchase. */
    private final ComparableCriterion<PurchaseItemCriteria, Long> userId = new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<PurchaseItemCriteria, Long> userId() {
        return userId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Shortcut for {@code userId().eq(value)}; {@code null} does not filter. */
    public PurchaseItemCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(purchaseItemId, purchaseId, productId, amount, price, userId);
    }

    // :: Alternative groups

    private List<PurchaseItemCriteria> anyOf = List.of();

    @Override
    public List<PurchaseItemCriteria> anyOf() {
        return anyOf;
    }

    /** Whether there are alternative groups. */
    public boolean hasAnyOf() {
        return !anyOf.isEmpty();
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public PurchaseItemCriteria withAnyOf(List<PurchaseItemCriteria> groups) {
        this.anyOf = groups == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(groups));
        return this;
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public PurchaseItemCriteria withAnyOf(PurchaseItemCriteria... groups) {
        return withAnyOf(Arrays.asList(groups));
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public PurchaseItemCriteria withOrderBy(OrderBy orderBy) {
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
        /** Order in which the items were added. */
        OLDEST_FIRST,
        /** Reverse order of addition. */
        NEWEST_FIRST,
        /** From highest to lowest line price. */
        MOST_EXPENSIVE_FIRST,
        /** From largest to smallest quantity. */
        LARGEST_QUANTITY_FIRST,
    }

}
