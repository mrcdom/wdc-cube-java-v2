package br.com.wdc.framework.domain.repository;

import java.util.List;

import br.com.wdc.framework.domain.pagination.Page;

/**
 * Contract of a repository that only reads: count, fetch, fetch a page and fetch by key.
 *
 * <p>
 * An entity that the application only reads (a report, a view of the database, a dashboard) implements this one and
 * has no writes to refuse. {@link Repository} extends it with the writes.
 * </p>
 *
 * @param <E> entity type
 * @param <C> search criteria type
 * @param <K> primary key type
 */
public interface ReadOnlyRepository<E, C, K> {

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
}
