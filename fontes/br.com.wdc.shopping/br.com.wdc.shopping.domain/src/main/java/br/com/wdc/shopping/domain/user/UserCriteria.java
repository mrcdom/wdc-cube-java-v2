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

    /** O valor é convertido em resumo MD5 antes de chegar à coluna — a comparação é sobre o resumo, e por isso só a igualdade dá resultado útil. */
    private final TextCriterion<UserCriteria> password = new TextCriterion<>(this, "password");

    public TextCriterion<UserCriteria> password() {
        return password;
    }

    /** Se há critério neste campo. É por aqui que a tradução pergunta. */
    public boolean hasPassword() {
        return password.isSet();
    }

    /** Atalho para {@code password().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withPassword(String value) {
        return value == null ? this : password().eq(value);
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
        return List.of(userId, userName, password, name, roles);
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

    public enum OrderBy {
        ASCENDING,
        DESCENDING
    }

}
