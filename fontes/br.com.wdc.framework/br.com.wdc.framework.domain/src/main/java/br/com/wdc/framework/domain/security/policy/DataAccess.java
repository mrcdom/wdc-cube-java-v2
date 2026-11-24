package br.com.wdc.framework.domain.security.policy;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * What one requester may do with an entity: the grants of their roles, applied to each request before it reaches the
 * repository, and to each row on its way out.
 *
 * <p>
 * Each method either adapts the request — restricts the rows, completes the projection — or refuses it with an
 * {@link AccessDeniedException} that names the fields at fault. Refusing is deliberate: a client whose request is
 * quietly trimmed shows blanks without knowing why, and a correct client never asks for what its roles do not allow.
 * </p>
 *
 * <ul>
 * <li><b>Reading</b> ({@link #prepareRead}): every projected field must be seen by some grant; every field the criteria
 * filters or sorts by must be read as it is by one grant, and only the rows of the grants that do so are returned; each
 * row then shows, per field, what the grants that reach that row allow — as it is, masked, or nothing.</li>
 * <li><b>Counting</b> ({@link #restrictCount}): the same rows as reading.</li>
 * <li><b>Creating</b> ({@link #checkCreate}): one grant must allow every field sent, and reach the new row.</li>
 * <li><b>Updating</b> ({@link #checkUpdate}): one grant must reach the stored row, allow every changed field, and
 * still reach the row after the change.</li>
 * <li><b>Deleting</b> ({@link #restrictDelete}): only the rows of grants that delete, filtered by fields they read.</li>
 * </ul>
 *
 * @param <E> the entity
 * @param <C> its criteria
 */
public class DataAccess<E, C extends Criteria> {

    /**
     * Shapes the rows a read returned: per row, the fields the grants that reach it allow.
     *
     * @param <E> the entity
     */
    public interface ReadPlan<E> {

        E apply(E row);

        default List<E> apply(List<E> rows) {
            for (var row : rows) {
                apply(row);
            }
            return rows;
        }
    }

    private final EntityDescriptor<E, C> entity;
    private final SecurityContext sc;
    private final List<DataGrant<E, C>> grants;

    DataAccess(EntityDescriptor<E, C> entity, SecurityContext sc, List<DataGrant<E, C>> grants) {
        this.entity = entity;
        this.sc = sc;
        this.grants = List.copyOf(grants);
    }

    static <E, C extends Criteria> DataAccess<E, C> unrestricted(EntityDescriptor<E, C> entity) {
        return new Unrestricted<>(entity);
    }

    // :: Reading

    /**
     * Adapts a read: checks the projection and the filtered fields, restricts the rows, and fetches what the row check
     * needs. Without a projection, the read gets every field some grant sees.
     *
     * @return the plan that shapes each returned row; apply it before writing the response
     */
    public ReadPlan<E> prepareRead(C criteria) {
        var readers = grantsWhere(DataGrant::reads);
        if (readers.isEmpty()) {
            throw denied("not allowed to read " + entity.name());
        }

        var requested = requestedFields(criteria, readers);
        var unseen = new LinkedHashSet<String>();
        for (var field : requested) {
            if (readers.stream().noneMatch(g -> g.sees(field))) {
                unseen.add(field);
            }
        }
        if (!unseen.isEmpty()) {
            throw denied("not allowed to read " + entity.name() + " fields " + unseen);
        }

        restrictRows(criteria, eligibleToFilter(criteria, readers));

        var fetched = new LinkedHashSet<>(requested);
        for (var grant : readers) {
            fetched.addAll(grant.rows().fields());
        }
        fetched.remove(entity.keyName());
        entity.setProjection(criteria, entity.projectionOf(fetched));

        return row -> shape(row, requested, readers);
    }

    /** Restricts a count to the rows a read with the same criteria would return. */
    public void restrictCount(C criteria) {
        var readers = grantsWhere(DataGrant::reads);
        if (readers.isEmpty()) {
            throw denied("not allowed to read " + entity.name());
        }
        restrictRows(criteria, eligibleToFilter(criteria, readers));
    }

    private Set<String> requestedFields(C criteria, List<DataGrant<E, C>> readers) {
        var projection = entity.projection(criteria);
        if (projection != null) {
            return entity.fieldsSetIn(projection);
        }
        var all = new LinkedHashSet<String>();
        for (var field : entity.fieldNames()) {
            if (readers.stream().anyMatch(g -> g.sees(field))) {
                all.add(field);
            }
        }
        return all;
    }

    /** The field values of a row: as they are, masked, or cleared, by the grants that reach it. */
    private E shape(E row, Set<String> requested, List<DataGrant<E, C>> readers) {
        if (row == null) {
            return null;
        }
        var reaching = new ArrayList<DataGrant<E, C>>();
        for (var grant : readers) {
            if (grant.rows().covers(row, sc)) {
                reaching.add(grant);
            }
        }
        for (var fieldName : entity.fieldNames()) {
            var field = entity.field(fieldName);
            if (!requested.contains(fieldName)) {
                field.clear(row);
                continue;
            }
            if (reaching.stream().anyMatch(g -> g.read().contains(fieldName))) {
                continue;
            }
            var mask = reaching.stream()
                    .map(g -> g.masked().get(fieldName))
                    .filter(m -> m != null)
                    .findFirst()
                    .orElse(null);
            if (mask != null) {
                field.mask(row, mask);
            } else {
                field.clear(row);
            }
        }
        return row;
    }

    // :: Writing

    /** Checks a new row: one grant must allow every field set in it and reach it. */
    public void checkCreate(E row) {
        var fields = entity.fieldsSetIn(row);
        var creators = grantsWhere(g -> !g.create().isEmpty());
        if (creators.isEmpty()) {
            throw denied("not allowed to create " + entity.name());
        }
        var allowed = creators.stream()
                .anyMatch(g -> g.create().containsAll(fields) && g.rows().covers(row, sc));
        if (!allowed) {
            throw denied("not allowed to create " + entity.name() + " with " + notIn(fields, creators, DataGrant::create));
        }
    }

    /**
     * Checks an update: one grant must reach the stored row, allow every changed field, and still reach the row once
     * changed — a grant over one's own rows cannot hand a row to someone else.
     *
     * @param row the new values, with the key
     * @param changes a projection of the changed fields
     * @param loader fetches the stored row by the key of {@code row}, with the given projection; {@code null} when it
     *        does not exist — then there is nothing to check, and the update changes nothing
     */
    public void checkUpdate(E row, E changes, BiFunction<E, E, E> loader) {
        var fields = entity.fieldsSetIn(changes);
        var updaters = grantsWhere(g -> !g.update().isEmpty());
        if (updaters.isEmpty()) {
            throw denied("not allowed to update " + entity.name());
        }
        if (entity.key().get(row) == null) {
            throw new InvalidRequestException("update of " + entity.name() + " without the key");
        }
        if (fields.isEmpty()) {
            return;
        }

        var scopeFields = new LinkedHashSet<String>();
        for (var grant : updaters) {
            scopeFields.addAll(grant.rows().fields());
        }
        scopeFields.remove(entity.keyName());
        var stored = loader.apply(row, entity.projectionOf(scopeFields));
        if (stored == null) {
            return;
        }

        var reaching = new ArrayList<DataGrant<E, C>>();
        for (var grant : updaters) {
            if (grant.rows().covers(stored, sc)) {
                reaching.add(grant);
            }
        }
        if (reaching.isEmpty()) {
            throw denied("not allowed to update this " + entity.name());
        }

        var changed = entity.newEntity();
        entity.key().copy(stored, changed);
        for (var fieldName : scopeFields) {
            entity.field(fieldName).copy(stored, changed);
        }
        for (var fieldName : fields) {
            entity.field(fieldName).copy(row, changed);
        }

        var allowed = reaching.stream()
                .anyMatch(g -> g.update().containsAll(fields) && g.rows().covers(changed, sc));
        if (!allowed) {
            throw denied("not allowed to update " + entity.name() + " " + notIn(fields, reaching, DataGrant::update));
        }
    }

    /** Restricts a delete to the rows of the grants that delete and read every field the criteria filters by. */
    public void restrictDelete(C criteria) {
        var deleters = grantsWhere(DataGrant::delete);
        if (deleters.isEmpty()) {
            throw denied("not allowed to delete " + entity.name());
        }
        restrictRows(criteria, eligibleToFilter(criteria, deleters));
    }

    // :: Rows

    /**
     * The grants that read, as they are, every field the criteria filters or sorts by. Filtering by a field reveals its
     * values, so it takes a grant that reads it; and the rows returned are only those of such grants.
     */
    private List<DataGrant<E, C>> eligibleToFilter(C criteria, List<DataGrant<E, C>> candidates) {
        var filtered = entity.filteredFields(criteria);
        var eligible = new ArrayList<DataGrant<E, C>>();
        for (var grant : candidates) {
            if (grant.read().containsAll(filtered)) {
                eligible.add(grant);
            }
        }
        if (eligible.isEmpty()) {
            throw denied("not allowed to filter or sort " + entity.name() + " by "
                    + notIn(filtered, candidates, DataGrant::read));
        }
        return eligible;
    }

    /**
     * Adds to the criteria the rows the grants reach: nothing when one of them reaches every row; otherwise one
     * alternative group per row scope, each carrying the groups the criteria already had, so those alternatives still
     * hold inside the reachable rows.
     */
    private void restrictRows(C criteria, List<DataGrant<E, C>> grants) {
        var scopes = new IdentityHashMap<RowScope<E, C>, Boolean>();
        for (var grant : grants) {
            if (grant.rows() == RowScope.ALL) {
                return;
            }
            scopes.put(grant.rows(), Boolean.TRUE);
        }
        var clientGroups = entity.anyOf(criteria);
        var groups = new ArrayList<C>(scopes.size());
        for (var scope : scopes.keySet()) {
            var group = entity.newCriteria();
            scope.restrict(group, sc);
            entity.setAnyOf(group, clientGroups);
            groups.add(group);
        }
        entity.setAnyOf(criteria, groups);
    }

    // :: Helpers

    private List<DataGrant<E, C>> grantsWhere(Predicate<DataGrant<E, C>> condition) {
        var selected = new ArrayList<DataGrant<E, C>>();
        for (var grant : grants) {
            if (condition.test(grant)) {
                selected.add(grant);
            }
        }
        return selected;
    }

    /**
     * The fields none of the grants declares; all of {@code fields} when each is declared by some grant — then what
     * fails is asking for them together, and the message names them all.
     */
    private Set<String> notIn(Set<String> fields, List<DataGrant<E, C>> grants,
            Function<DataGrant<E, C>, Set<String>> declared) {
        var missing = new LinkedHashSet<String>();
        for (var field : fields) {
            if (grants.stream().noneMatch(g -> declared.apply(g).contains(field))) {
                missing.add(field);
            }
        }
        return missing.isEmpty() ? fields : missing;
    }

    private AccessDeniedException denied(String message) {
        return new AccessDeniedException(message);
    }

    /** Security off: every request passes as it came. */
    private static final class Unrestricted<E, C extends Criteria> extends DataAccess<E, C> {

        Unrestricted(EntityDescriptor<E, C> entity) {
            super(entity, null, List.of());
        }

        @Override
        public ReadPlan<E> prepareRead(C criteria) {
            return row -> row;
        }

        @Override
        public void restrictCount(C criteria) {
            // unrestricted
        }

        @Override
        public void checkCreate(E row) {
            // unrestricted
        }

        @Override
        public void checkUpdate(E row, E changes, BiFunction<E, E, E> loader) {
            // unrestricted
        }

        @Override
        public void restrictDelete(C criteria) {
            // unrestricted
        }
    }
}
