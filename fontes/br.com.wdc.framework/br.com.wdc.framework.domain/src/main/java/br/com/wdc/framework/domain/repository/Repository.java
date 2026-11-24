package br.com.wdc.framework.domain.repository;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import br.com.wdc.framework.domain.pagination.Page;

/**
 * Base read contract for SGS repositories.
 *
 * @param <E> entity type
 * @param <C> search criteria type
 * @param <K> primary key type
 */
public interface Repository<E, C, K> {

    boolean insert(E bean);

    /**
     * Updates an entity.
     *
     * @param newBean    new values
     * @param oldBean    old values for optimistic locking (may be {@code null} to skip it)
     * @param projection tells which fields to consider — fields that are non-null in the projection are updated
     */
    boolean update(E newBean, E oldBean, E projection);

    /** Updates considering only the non-null fields of newBean (backward-compatible). */
    default boolean update(E newBean) {
        return update(newBean, null, null);
    }

    /** Updates considering only the non-null fields of newBean (backward-compatible). */
    default boolean update(E newBean, E oldBean) {
        return update(newBean, oldBean, null);
    }

    /** Inserts if {@code oldBean} is {@code null}; updates otherwise. */
    default boolean insertOrUpdate(E newBean, E oldBean) {
        return oldBean == null ? insert(newBean) : update(newBean, oldBean);
    }

    int delete(C criteria);

    int count(C criteria);

    List<E> fetch(C criteria, int offset, int limit);

    /** Fetches with no limit on the number of rows. */
    default List<E> fetch(C criteria) {
        return fetch(criteria, 0, 0);
    }

    /** Fetches with a limit and no offset (e.g. autocomplete, "top N"). */
    default List<E> fetch(C criteria, int limit) {
        return fetch(criteria, 0, limit);
    }

    default Page<E> fetchPage(C criteria, int page, int pageSize) {
        var total = count(criteria);
        var items = fetch(criteria, page * pageSize, pageSize);
        return Page.of(items, page, pageSize, total);
    }

    /**
     * Fetches by key. Implemented in each entity's interface, as a {@code default} that builds the
     * {@code XxxCriteria} with equality on the key and delegates to {@code fetch}.
     *
     * @param projection {@code null} projects {@link #newProjection()}.
     * @return the entity, or {@code null} if there is no row with that key.
     */
    E fetchById(K id, E projection);

    E newProjection();

    /**
     * Checks whether a field must be included in the UPDATE.
     * Returns {@code true} when the projection marks the field (non-null) AND the value changed relative to oldBean.
     *
     * @param newBean    entity with the new values
     * @param oldBean    entity with the old values (may be {@code null})
     * @param projection projection marking the fields to update
     * @param getter     extracts the value of the field of interest
     */
    static <E, V> boolean changed(E newBean, E oldBean, E projection, Function<E, V> getter) {
        return getter.apply(projection) != null
                && (oldBean == null || !Objects.equals(getter.apply(newBean), getter.apply(oldBean)));
    }
}
