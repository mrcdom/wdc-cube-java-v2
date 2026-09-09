package br.com.wdc.shopping.domain.product;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;

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

    private final ComparableCriterion<ProductCriteria, Long> productId = new ComparableCriterion<>(this, "productId");

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

    private final TextCriterion<ProductCriteria> name = new TextCriterion<>(this, "name");

    public TextCriterion<ProductCriteria> name() {
        return name;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasName() {
        return name.isSet();
    }

    /** Atalho para {@code name().eq(valor)}; {@code null} não filtra. */
    public ProductCriteria withName(String value) {
        return value == null ? this : name().eq(value);
    }

    /** O valor é convertido em {@code BigDecimal} antes de chegar à coluna. */
    private final ComparableCriterion<ProductCriteria, Double> price = new ComparableCriterion<>(this, "price");

    public ComparableCriterion<ProductCriteria, Double> price() {
        return price;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasPrice() {
        return price.isSet();
    }

    /** Atalho para {@code price().eq(valor)}; {@code null} não filtra. */
    public ProductCriteria withPrice(Double value) {
        return value == null ? this : price().eq(value);
    }

    private final TextCriterion<ProductCriteria> description = new TextCriterion<>(this, "description");

    public TextCriterion<ProductCriteria> description() {
        return description;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasDescription() {
        return description.isSet();
    }

    /** Atalho para {@code description().eq(valor)}; {@code null} não filtra. */
    public ProductCriteria withDescription(String value) {
        return value == null ? this : description().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(productId, name, price, description);
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
        /** Ordem de cadastro — o mais antigo primeiro. */
        OLDEST_FIRST,
        /** Ordem de cadastro invertida — o cadastrado por último aparece primeiro. */
        NEWEST_FIRST,
        /** Alfabética pelo nome do produto. */
        NAME_A_TO_Z,
        /** Do menor para o maior preço. */
        CHEAPEST_FIRST,
        /** Do maior para o menor preço. */
        MOST_EXPENSIVE_FIRST,
    }

}
