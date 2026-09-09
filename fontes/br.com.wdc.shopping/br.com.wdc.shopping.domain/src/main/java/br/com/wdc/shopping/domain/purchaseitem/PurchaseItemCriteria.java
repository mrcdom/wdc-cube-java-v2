package br.com.wdc.shopping.domain.purchaseitem;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;

/**
 * Critério de pesquisa de {@link PurchaseItem}.
 *
 * <p>
 * {@code userId} não é coluna desta tabela — é do dono da compra. A tradução o resolve com {@code EXISTS} sobre
 * {@code EN_PURCHASE}; para quem monta o filtro, é apenas mais um campo.
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

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId =
            new ComparableCriterion<>(this, "purchaseItemId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId() {
        return purchaseItemId;
    }

    public boolean hasPurchaseItemId() {
        return purchaseItemId.isSet();
    }

    /** Atalho para {@code purchaseItemId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withPurchaseItemId(Long value) {
        return value == null ? this : purchaseItemId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseId =
            new ComparableCriterion<>(this, "purchaseId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseId() {
        return purchaseId;
    }

    public boolean hasPurchaseId() {
        return purchaseId.isSet();
    }

    /** Atalho para {@code purchaseId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withPurchaseId(Long value) {
        return value == null ? this : purchaseId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> productId =
            new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<PurchaseItemCriteria, Long> productId() {
        return productId;
    }

    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Atalho para {@code productId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    /** Itens de compras do usuário — resolvido por {@code EXISTS} sobre a compra. */
    private final ComparableCriterion<PurchaseItemCriteria, Long> userId =
            new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<PurchaseItemCriteria, Long> userId() {
        return userId;
    }

    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Atalho para {@code userId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(purchaseItemId, purchaseId, productId, userId);
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

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }

}
