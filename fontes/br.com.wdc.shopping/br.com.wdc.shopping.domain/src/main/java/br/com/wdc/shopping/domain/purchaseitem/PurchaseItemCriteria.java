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

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId = new ComparableCriterion<>(this, "purchaseItemId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseItemId() {
        return purchaseItemId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasPurchaseItemId() {
        return purchaseItemId.isSet();
    }

    /** Atalho para {@code purchaseItemId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withPurchaseItemId(Long value) {
        return value == null ? this : purchaseItemId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> purchaseId = new ComparableCriterion<>(this, "purchaseId");

    public ComparableCriterion<PurchaseItemCriteria, Long> purchaseId() {
        return purchaseId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasPurchaseId() {
        return purchaseId.isSet();
    }

    /** Atalho para {@code purchaseId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withPurchaseId(Long value) {
        return value == null ? this : purchaseId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Long> productId = new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<PurchaseItemCriteria, Long> productId() {
        return productId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Atalho para {@code productId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    private final ComparableCriterion<PurchaseItemCriteria, Integer> amount = new ComparableCriterion<>(this, "amount");

    public ComparableCriterion<PurchaseItemCriteria, Integer> amount() {
        return amount;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasAmount() {
        return amount.isSet();
    }

    /** Atalho para {@code amount().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withAmount(Integer value) {
        return value == null ? this : amount().eq(value);
    }

    /** O valor é convertido em {@code BigDecimal} antes de chegar à coluna. */
    private final ComparableCriterion<PurchaseItemCriteria, Double> price = new ComparableCriterion<>(this, "price");

    public ComparableCriterion<PurchaseItemCriteria, Double> price() {
        return price;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasPrice() {
        return price.isSet();
    }

    /** Atalho para {@code price().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withPrice(Double value) {
        return value == null ? this : price().eq(value);
    }

    /** Itens de compras do usuário — resolvido por {@code EXISTS} sobre a compra. */
    private final ComparableCriterion<PurchaseItemCriteria, Long> userId = new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<PurchaseItemCriteria, Long> userId() {
        return userId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Atalho para {@code userId().eq(valor)}; {@code null} não filtra. */
    public PurchaseItemCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(purchaseItemId, purchaseId, productId, amount, price, userId);
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
     * Ordenações provisionadas.
     *
     * <p>
     * Cada constante é uma <b>ordenação inteira</b>, e não o pedido para ordenar por um campo: o nome diz o efeito
     * que se obtém, e a tradução — no repositório — decide por quais colunas e em que sentido isso se faz. Não há
     * composição; escolhe-se uma.
     * </p>
     *
     * <p>
     * <b>A lista é curta de propósito.</b> Ordenação nova entra por solicitação, e entra junto com o índice que a
     * sustenta — é o que mantém explícito o que o banco precisa aguentar. Oferecer ordenação livre por qualquer campo
     * pareceria generoso e produziria varredura completa na primeira consulta grande.
     * </p>
     */
    public enum OrderBy {
        /** Ordem de inclusão do item. */
        OLDEST_FIRST,
        /** Ordem de inclusão invertida. */
        NEWEST_FIRST,
        /** Do maior para o menor preço de linha. */
        MOST_EXPENSIVE_FIRST,
        /** Da maior para a menor quantidade. */
        LARGEST_QUANTITY_FIRST,
    }

}
