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

    public boolean hasUserName() {
        return userName.isSet();
    }

    /** Atalho para {@code userName().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withUserName(String value) {
        return value == null ? this : userName().eq(value);
    }

    private final TextCriterion<UserCriteria> password = new TextCriterion<>(this, "password");

    public TextCriterion<UserCriteria> password() {
        return password;
    }

    public boolean hasPassword() {
        return password.isSet();
    }

    /** Atalho para {@code password().eq(valor)}; {@code null} não filtra. */
    public UserCriteria withPassword(String value) {
        return value == null ? this : password().eq(value);
    }

    @Override
    public List<Criterion<?, ?>> criterions() {
        return List.of(userId, userName, password);
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
