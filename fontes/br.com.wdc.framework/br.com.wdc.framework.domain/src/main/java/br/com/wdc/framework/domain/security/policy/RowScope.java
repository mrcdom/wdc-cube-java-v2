package br.com.wdc.framework.domain.security.policy;

import java.util.Set;

import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * Which rows of an entity a {@link DataGrant} reaches — all of them, or those related to the requester (their own
 * user, their purchases).
 *
 * <p>
 * It is said twice, in two languages, and both must agree: {@link #restrict} as criteria, so the database returns only
 * the reachable rows; {@link #covers} in memory, so each returned row gets the fields of the grants that reach it. A
 * user with two roles may reach a row through one and not the other, and the fields of that row are the ones of the
 * grants that reach it — not the union of everything the user may read anywhere.
 * </p>
 *
 * <p>
 * A policy compares scopes by identity: declare each one once, as a constant, and share it among the grants.
 * </p>
 *
 * @param <E> the entity
 * @param <C> its criteria
 */
public interface RowScope<E, C> {

    /** Every row. */
    @SuppressWarnings("rawtypes")
    RowScope ALL = new RowScope() {

        @Override
        public void restrict(Object group, SecurityContext sc) {
            // every row: nothing to restrict
        }

        @Override
        public boolean covers(Object row, SecurityContext sc) {
            return true;
        }

        @Override
        public Set<String> fields() {
            return Set.of();
        }

        @Override
        public String toString() {
            return "ALL";
        }
    };

    @SuppressWarnings("unchecked")
    static <E, C> RowScope<E, C> all() {
        return ALL;
    }

    /** Sets on {@code group} — an empty criteria — the fields that select the reachable rows. */
    void restrict(C group, SecurityContext sc);

    /** Whether {@code row} is reachable; it reads only the {@link #fields()}, which are fetched for that. */
    boolean covers(E row, SecurityContext sc);

    /** The fields {@link #covers} reads. */
    Set<String> fields();
}
