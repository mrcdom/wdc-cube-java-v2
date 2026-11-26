package br.com.wdc.framework.domain.repository;

import java.util.Objects;
import java.util.function.Function;

/**
 * Contract of a repository that reads and writes: the reads of {@link ReadOnlyRepository}, plus insert, update and
 * delete.
 *
 * @param <E> entity type
 * @param <C> search criteria type
 * @param <K> primary key type
 */
public interface Repository<E, C, K> extends ReadOnlyRepository<E, C, K> {

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
