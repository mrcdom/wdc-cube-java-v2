package br.com.wdc.shopping.domain.product;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;

/**
 * Critério de pesquisa de {@link Product}.
 *
 * <p>
 * <b>Os campos nascem com o critério</b>, e não sob demanda: {@code productId()} nunca devolve {@code null}, e o que
 * distingue "não filtra" de "filtra" é {@link Criterion#isSet()}. Entidade de dezenas de colunas pagaria por
 * pré-criar todos, e aí valeria criá-los na primeira necessidade; com meia dúzia de campos, o custo não se mede e o
 * código fica sem um {@code if} por acesso.
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

    private final ComparableCriterion<ProductCriteria, Long> productId =
            new ComparableCriterion<>(this, "productId");

    public ComparableCriterion<ProductCriteria, Long> productId() {
        return productId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasProductId() {
        return productId.isSet();
    }

    /** Atalho para {@code productId().eq(valor)}; {@code null} não filtra. */
    public ProductCriteria withProductId(Long value) {
        return value == null ? this : productId().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(productId);
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

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }

}
