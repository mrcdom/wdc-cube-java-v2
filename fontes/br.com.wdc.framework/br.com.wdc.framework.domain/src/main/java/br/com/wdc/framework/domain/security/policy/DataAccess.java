package br.com.wdc.framework.domain.security.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;
import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.exception.AccessDeniedException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.projection.ProjectionList;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.security.policy.EntityDescriptor.Kind;

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
 * row then shows, per field, what the grants that reach that row allow — as it is, masked, or nothing. What the
 * projection asks <b>inside</b> a relation or a collection goes through the policy of that entity, row by row; the rows
 * of a collection are restricted by it too.</li>
 * <li><b>Counting</b> ({@link #restrictCount}): the same rows as reading.</li>
 * <li><b>Creating</b> ({@link #checkCreate}): one grant must allow every field sent, and reach the new row; a relation
 * is written by its key only, and each row of a collection is created through the policy of its entity.</li>
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
     * needs.
     *
     * <p>
     * Without a projection, the read gets the entity's default shape ({@code EntityDescriptor.defaultShape}),
     * <b>trimmed</b> to what the requester may read instead of refused: the client asked for nothing in particular, so
     * nothing it asked for is forbidden.
     * </p>
     *
     * @return the plan that shapes each returned row; apply it before writing the response
     */
    public ReadPlan<E> prepareRead(C criteria) {
        var requested = entity.projection(criteria);
        var shape = requested != null
                ? shape(requested, criteria, false)
                : shape(entity.defaultShape(), criteria, true);
        entity.setProjection(criteria, shape.fetch);
        return shape::apply;
    }

    /** Restricts a count to the rows a read with the same criteria would return. */
    public void restrictCount(C criteria) {
        restrictRows(criteria, eligibleToFilter(criteria, readers()));
    }

    /**
     * One level of a read: what is asked of this entity, the rows the criteria may reach (when there is a criteria — a
     * relation has none: its row is the one the owner points to), and what to fetch.
     *
     * @param trim whether what the requester may not read is left out (the default shape) instead of refused (a
     *        projection the client sent)
     */
    private Shape shape(E requested, C criteria, boolean trim) {
        var readers = readers();
        var fields = entity.fieldsSetIn(requested);

        var unseen = new LinkedHashSet<String>();
        for (var field : fields) {
            if (readers.stream().noneMatch(g -> g.sees(field))) {
                unseen.add(field);
            }
        }
        if (trim) {
            fields.removeAll(unseen);
        } else if (!unseen.isEmpty()) {
            throw denied("not allowed to read " + entity.name() + " fields " + unseen);
        }

        if (criteria != null) {
            restrictRows(criteria, eligibleToFilter(criteria, readers));
        }

        var fetch = entity.newEntity();
        entity.key().mark(fetch);
        var nested = new HashMap<String, DataAccess<?, ?>.Shape>();
        var unreadable = new LinkedHashSet<String>();
        for (var fieldName : fields) {
            var field = entity.field(fieldName);
            var inside = field.get(requested);
            switch (field.kind()) {
            case VALUE -> field.mark(fetch);
            case RELATION -> {
                var access = field.targetPolicy().accessFor(sc);
                if (inside != null && !field.targetEntity().fieldsSetIn(inside).isEmpty()
                        && !(trim && access.readsNothing())) {
                    var shape = access.shape(inside, null, trim);
                    field.setRaw(fetch, shape.fetch);
                    nested.put(fieldName, shape);
                } else {
                    field.mark(fetch);
                }
            }
            case COLLECTION -> {
                var access = field.targetPolicy().accessFor(sc);
                if (trim && access.readsNothing()) {
                    unreadable.add(fieldName);
                } else if (inside instanceof List<?> list && !list.isEmpty()) {
                    var childCriteria = list instanceof HasCriteria hc && hc.getCriteria() instanceof Criteria c
                            ? c
                            : field.targetEntity().newCriteria();
                    var shape = access.shape(list.get(0), childCriteria, trim);
                    var children = new ProjectionList<>(shape.fetch, childCriteria);
                    if (list instanceof HasSlice slice) {
                        children.withLimit(slice.getLimit())
                                .withOffset(slice.getOffset());
                    }
                    field.setRaw(fetch, children);
                    nested.put(fieldName, shape);
                } else {
                    field.mark(fetch);
                }
            }
            }
        }
        fields.removeAll(unreadable);
        for (var grant : readers) {
            entity.merge(fetch, grant.rows().projection());
        }
        return new Shape(fields, readers, nested, fetch);
    }

    /** Whether no grant reads this entity — then, in a default shape, a relation to it goes by key, a collection out. */
    private boolean readsNothing() {
        return grantsWhere(DataGrant::reads).isEmpty();
    }

    /** A planned level of a read, and how it shapes each returned row. */
    private final class Shape {

        final Set<String> fields;
        final List<DataGrant<E, C>> readers;
        final Map<String, DataAccess<?, ?>.Shape> nested;
        final E fetch;

        Shape(Set<String> fields, List<DataGrant<E, C>> readers, Map<String, DataAccess<?, ?>.Shape> nested, E fetch) {
            this.fields = fields;
            this.readers = readers;
            this.nested = nested;
            this.fetch = fetch;
        }

        /** The field values of a row: as they are, masked, or cleared, by the grants that reach it. */
        E apply(E row) {
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
                if (!fields.contains(fieldName)) {
                    field.clear(row);
                    continue;
                }
                var readAsItIs = reaching.stream().anyMatch(g -> g.read().contains(fieldName));
                if (field.kind() != Kind.VALUE) {
                    if (readAsItIs) {
                        shapeRelated(fieldName, field, row);
                    } else {
                        field.clear(row);
                    }
                    continue;
                }
                if (readAsItIs) {
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

        /** The related row (or rows): through the related entity's plan, or reduced to the key when only it was asked. */
        private void shapeRelated(String fieldName, EntityDescriptor.Field<E, ?> field, E row) {
            var inside = nested.get(fieldName);
            var value = field.get(row);
            if (value instanceof List<?> children) {
                for (var child : children) {
                    shapeOne(inside, field, child);
                }
            } else if (value != null) {
                shapeOne(inside, field, value);
            }
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void shapeOne(DataAccess<?, ?>.Shape inside, EntityDescriptor.Field<E, ?> field, Object related) {
            if (inside != null) {
                ((DataAccess.Shape) inside).apply(related);
            } else {
                field.targetEntity().keepKeyOnly(related);
            }
        }
    }

    // :: Writing

    /**
     * Checks a new row: one grant must allow every field set in it and reach it. A relation is written by its key
     * only; each row of a collection — the items of a new purchase — is checked as a new row of its entity, belonging
     * to this one.
     */
    public void checkCreate(E row) {
        checkCreate(row, null);
    }

    /** @param owner the relation that points to the row this one is created inside, set by the owner's check */
    private void checkCreate(E row, String owner) {
        var fields = entity.fieldsSetIn(row);
        var creators = grantsWhere(g -> !g.create().isEmpty());
        if (creators.isEmpty()) {
            throw denied("not allowed to create " + entity.name());
        }
        requireRelationsByKey(row, fields, owner);
        var allowed = creators.stream()
                .anyMatch(g -> g.create().containsAll(fields) && g.rows().covers(row, sc));
        if (!allowed) {
            throw denied("not allowed to create " + entity.name() + " with " + notIn(fields, creators, DataGrant::create));
        }
        for (var fieldName : fields) {
            var field = entity.field(fieldName);
            if (field.kind() == Kind.COLLECTION) {
                checkCreateChildren(field, row);
            }
        }
    }

    private void checkCreateChildren(EntityDescriptor.Field<E, ?> field, E row) {
        var inverse = field.targetEntity().field(field.inverse());
        var access = field.targetPolicy().accessFor(sc);
        for (var child : (List<?>) field.get(row)) {
            inverse.setRaw(child, row);
            access.checkCreate(child, inverse.name());
        }
    }

    /** A relation sent with more than its key would look like a write of the related row, which it is not. */
    private void requireRelationsByKey(E row, Set<String> fields, String owner) {
        for (var fieldName : fields) {
            var field = entity.field(fieldName);
            if (field.kind() == Kind.RELATION && !fieldName.equals(owner)
                    && !field.targetEntity().fieldsSetIn(field.get(row)).isEmpty()) {
                throw denied(entity.name() + "." + fieldName + " is written by its key only");
            }
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
        requireRelationsByKey(row, fields, null);

        var scope = entity.projectionOf(Set.of());
        for (var grant : updaters) {
            entity.merge(scope, grant.rows().projection());
        }
        var stored = loader.apply(row, scope);
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
        for (var fieldName : entity.fieldsSetIn(scope)) {
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

    private List<DataGrant<E, C>> readers() {
        var readers = grantsWhere(DataGrant::reads);
        if (readers.isEmpty()) {
            throw denied("not allowed to read " + entity.name());
        }
        return readers;
    }

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
        var existing = entity.anyOf(criteria);
        var groups = new ArrayList<C>(scopes.size());
        for (var scope : scopes.keySet()) {
            var group = entity.newCriteria();
            scope.restrict(group, sc);
            entity.setAnyOf(group, existing);
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
