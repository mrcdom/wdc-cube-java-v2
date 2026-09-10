package br.com.wdc.shopping.domain.user;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;

/**
 * Critério de pesquisa de {@link User}.
 *
 * <p>
 * O tipo do campo decide o que se pode pedir dele: {@code userId} é ordenável, {@code userName} é textual e aceita
 * também {@code startingWith} e {@code containing}. É o que impede na compilação um {@code between} sobre um campo
 * que não tem ordem útil.
 * </p>
 */
public class UserCriteria implements Criteria {

    // :: Projection

    private User projection;

    public User projection() {
        return projection;
    }

    public UserCriteria withProjection(User projection) {
        this.projection = projection;
        return this;
    }

    // :: Criteria

    private final ComparableCriterion<UserCriteria, Long> userId = new ComparableCriterion<>(this, "userId");

    public ComparableCriterion<UserCriteria, Long> userId() {
        return userId;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Atalho para {@code userId().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    private final TextCriterion<UserCriteria> userName = new TextCriterion<>(this, "userName");

    public TextCriterion<UserCriteria> userName() {
        return userName;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasUserName() {
        return userName.isSet();
    }

    /** Atalho para {@code userName().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withUserName(String value) {
        return value == null ? this : userName().eq(value);
    }

    private final TextCriterion<UserCriteria> name = new TextCriterion<>(this, "name");

    public TextCriterion<UserCriteria> name() {
        return name;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasName() {
        return name.isSet();
    }

    /** Atalho para {@code name().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withName(String value) {
        return value == null ? this : name().eq(value);
    }

    /** Papéis separados por vírgula na coluna; {@code containing("ADMIN")} é o filtro usual. */
    private final TextCriterion<UserCriteria> roles = new TextCriterion<>(this, "roles");

    public TextCriterion<UserCriteria> roles() {
        return roles;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasRoles() {
        return roles.isSet();
    }

    /** Atalho para {@code roles().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withRoles(String value) {
        return value == null ? this : roles().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(userId, userName, name, roles);
    }

    // :: Order By

    private OrderBy orderBy;

    public OrderBy orderBy() {
        return orderBy;
    }

    public UserCriteria withOrderBy(OrderBy orderBy) {
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
        /** Ordem de cadastro invertida. */
        NEWEST_FIRST,
        /** Alfabética pelo nome da pessoa. */
        NAME_A_TO_Z,
        /** Alfabética pelo login. */
        LOGIN_A_TO_Z,
    }

}
