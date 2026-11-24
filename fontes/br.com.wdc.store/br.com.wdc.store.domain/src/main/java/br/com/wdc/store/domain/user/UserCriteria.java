package br.com.wdc.store.domain.user;

import java.util.List;

import br.com.wdc.framework.domain.criteria.ComparableCriterion;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.criteria.TextCriterion;

/**
 * Search criteria for {@link User}.
 *
 * <p>
 * The field type decides what can be asked of it: {@code userId} is comparable, {@code userName} is textual and also
 * accepts {@code startingWith} and {@code containing}. That is what rules out, at compile time, a {@code between} on a
 * field that has no useful ordering.
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

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasUserId() {
        return userId.isSet();
    }

    /** Shortcut for {@code userId().eq(value)}; {@code null} does not filter. */
    public UserCriteria withUserId(Long value) {
        return value == null ? this : userId().eq(value);
    }

    private final TextCriterion<UserCriteria> userName = new TextCriterion<>(this, "userName");

    public TextCriterion<UserCriteria> userName() {
        return userName;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasUserName() {
        return userName.isSet();
    }

    /** Shortcut for {@code userName().eq(value)}; {@code null} does not filter. */
    public UserCriteria withUserName(String value) {
        return value == null ? this : userName().eq(value);
    }

    private final TextCriterion<UserCriteria> name = new TextCriterion<>(this, "name");

    public TextCriterion<UserCriteria> name() {
        return name;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasName() {
        return name.isSet();
    }

    /** Shortcut for {@code name().eq(value)}; {@code null} does not filter. */
    public UserCriteria withName(String value) {
        return value == null ? this : name().eq(value);
    }

    /** Comma-separated roles in the column; {@code containing("ADMIN")} is the usual filter. */
    private final TextCriterion<UserCriteria> roles = new TextCriterion<>(this, "roles");

    public TextCriterion<UserCriteria> roles() {
        return roles;
    }

    /** Whether this field has a criterion. This is what the translation checks. */
    public boolean hasRoles() {
        return roles.isSet();
    }

    /** Shortcut for {@code roles().eq(value)}; {@code null} does not filter. */
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
        /** Reverse registration order. */
        NEWEST_FIRST,
        /** Alphabetical by the person's name. */
        NAME_A_TO_Z,
        /** Alphabetical by login. */
        LOGIN_A_TO_Z,
    }

}
