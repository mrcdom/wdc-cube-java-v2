package br.com.wdc.store.domain.product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;

/**
 * Search criteria for {@link Product}.
 *
 * <p>
 * <b>The fields are created with the criteria</b>, not on demand: {@code productId()} never returns {@code null}, and
 * what tells "does not filter" from "filters" is {@link Criterion#isSet()}. An entity with dozens of columns would pay
 * for pre-creating them all, and then creating them on first use would be worth it; with half a dozen fields, the cost
 * is negligible and the code is spared an {@code if} on every access.
 * </p>
 */
public class ProductCriteria implements Criteria {

    // :: Projection

    private Product projection;

    public Product projection() {
        return projection;
    }

    public ProductCriteria withProjection(Product projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private final ComparableCriterion<ProductCriteria, Long> productId = new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<ProductCriteria, Long> productId() {
        return productId;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Shortcut for {@code productId().eq(value)}; {@code null} does not filter. */
    public ProductCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    private final TextCriterion<ProductCriteria> name = new TextCriterion<>(this, "name");

    public TextCriterion<ProductCriteria> name() {
        return name;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasName() {
        return name.isSet();
    }

    /** Shortcut for {@code name().eq(value)}; {@code null} does not filter. */
    public ProductCriteria withName(String value) {
        return value == null ? this : name().eq(value);
    }

    /** The value is converted to {@code BigDecimal} before it reaches the column. */
    private final ComparableCriterion<ProductCriteria, Double> price = new ComparableCriterion<>(this, "price");

    public ComparableCriterion<ProductCriteria, Double> price() {
        return price;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasPrice() {
        return price.isSet();
    }

    /** Shortcut for {@code price().eq(value)}; {@code null} does not filter. */
    public ProductCriteria withPrice(Double value) {
        return value == null ? this : price().eq(value);
    }

    private final TextCriterion<ProductCriteria> description = new TextCriterion<>(this, "description");

    public TextCriterion<ProductCriteria> description() {
        return description;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasDescription() {
        return description.isSet();
    }

    /** Shortcut for {@code description().eq(value)}; {@code null} does not filter. */
    public ProductCriteria withDescription(String value) {
        return value == null ? this : description().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(productId, name, price, description);
    }

    // :: Alternative groups

    private List<ProductCriteria> anyOf = List.of();

    @Override
    public List<ProductCriteria> anyOf() {
        return anyOf;
    }

    /** Whether there are alternative groups. */
    public boolean hasAnyOf() {
        return !anyOf.isEmpty();
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public ProductCriteria withAnyOf(List<ProductCriteria> groups) {
        this.anyOf = groups == null ? List.of() : Collections.unmodifiableList(new ArrayList<>(groups));
        return this;
    }

    /** The alternative groups (see {@link Criteria#anyOf()}); replaces the previous ones. */
    public ProductCriteria withAnyOf(ProductCriteria... groups) {
        return withAnyOf(Arrays.asList(groups));
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public ProductCriteria withOrderBy(OrderBy orderBy) {
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
        /** Registration order — oldest first. */
        OLDEST_FIRST,
        /** Reverse registration order — the most recently registered comes first. */
        NEWEST_FIRST,
        /** Alphabetical by product name. */
        NAME_A_TO_Z,
        /** From lowest to highest price. */
        CHEAPEST_FIRST,
        /** From highest to lowest price. */
        MOST_EXPENSIVE_FIRST,
    }

}
