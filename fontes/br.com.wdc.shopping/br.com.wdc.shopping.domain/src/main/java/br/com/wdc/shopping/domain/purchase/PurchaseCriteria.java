package br.com.wdc.shopping.domain.purchase;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;

/**
 * Critério de pesquisa de {@link Purchase}.
 *
 * <p>
 * {@code userId} e {@code productId} não são colunas desta tabela: o primeiro é a chave estrangeira, o segundo só
 * existe nos itens. A tradução resolve cada um à sua maneira, mas o critério não precisa saber disso — quem o monta
 * pede pelo que a compra tem a ver, não por onde o dado está guardado.
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

    private final ComparableCriterion<PurchaseCriteria, Long> purchaseId =
            new ComparableCriterion<>(this, "purchaseId");

    public ComparableCriterion<PurchaseCriteria, Long> purchaseId() {
        return purchaseId;
    }

    public boolean hasPurchaseId() {
        return purchaseId.isSet();
    }

    /** Atalho para {@code purchaseId().eq(valor)}; {@code null} não filtra. */
    public PurchaseCriteria withPurchaseId(Long value) {
        return value == null ? this : purchaseId().eq(value);
    }

    private final ComparableCriterion<PurchaseCriteria, Long> userId = new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<PurchaseCriteria, Long> userId() {
        return userId;
    }

    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Atalho para {@code userId().eq(valor)}; {@code null} não filtra. */
    public PurchaseCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    /** Compras que contêm o produto — resolvido por {@code EXISTS} sobre os itens. */
    private final ComparableCriterion<PurchaseCriteria, Long> productId =
            new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<PurchaseCriteria, Long> productId() {
        return productId;
    }

    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Atalho para {@code productId().eq(valor)}; {@code null} não filtra. */
    public PurchaseCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(purchaseId, userId, productId);
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

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }
}
