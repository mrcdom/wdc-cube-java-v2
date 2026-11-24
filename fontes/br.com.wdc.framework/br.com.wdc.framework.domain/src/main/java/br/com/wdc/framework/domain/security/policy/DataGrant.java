package br.com.wdc.framework.domain.security.policy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import br.com.wdc.framework.domain.criteria.Criteria;

/**
 * What one role may do with one entity, over one set of rows: which fields it reads as they are, which it reads masked,
 * which it may set when creating and when updating, and whether it may delete.
 *
 * <p>
 * The fields are declared with projections of the entity — the same objects a query uses to say what it wants — so a
 * renamed field breaks the policy at compile time. A role that reaches different rows with different fields (its own
 * user in full, the others partially) has one grant for each.
 * </p>
 *
 * <pre>
 * g -&gt; g.rows(OWN)
 *         .read(new User()
 *                 .withUserName(pv.str)
 *                 .withName(pv.str))
 *         .update(new User()
 *                 .withName(pv.str))
 * </pre>
 *
 * <p>
 * The key is not declared: whoever reads a row reads its key, and it is never written — it identifies the row.
 * </p>
 *
 * @param <E> the entity
 * @param <C> its criteria
 */
public final class DataGrant<E, C extends Criteria> {

    private final RowScope<E, C> rows;
    private final Set<String> read;
    private final Map<String, Mask> masked;
    private final Set<String> create;
    private final Set<String> update;
    private final boolean delete;

    private DataGrant(Builder<E, C> b) {
        this.rows = b.rows;
        this.read = Collections.unmodifiableSet(new LinkedHashSet<>(b.read));
        this.masked = Collections.unmodifiableMap(new LinkedHashMap<>(b.masked));
        this.create = Collections.unmodifiableSet(new LinkedHashSet<>(b.create));
        this.update = Collections.unmodifiableSet(new LinkedHashSet<>(b.update));
        this.delete = b.delete;
    }

    public RowScope<E, C> rows() {
        return rows;
    }

    /** The fields read as they are. They are also the ones the grant may filter and sort by. */
    public Set<String> read() {
        return read;
    }

    /** The fields read through a mask. */
    public Map<String, Mask> masked() {
        return masked;
    }

    public Set<String> create() {
        return create;
    }

    public Set<String> update() {
        return update;
    }

    public boolean delete() {
        return delete;
    }

    /** Whether the grant reads the entity at all. */
    public boolean reads() {
        return !read.isEmpty() || !masked.isEmpty();
    }

    /** Whether it reads the field, as it is or masked. */
    boolean sees(String field) {
        return read.contains(field) || masked.containsKey(field);
    }

    /**
     * Declares a grant. The builder rejects what would contradict the policy: masking a field that is not textual,
     * reading the same field both ways, or writing a field that is read masked.
     */
    public static final class Builder<E, C extends Criteria> {

        private final EntityDescriptor<E, C> entity;
        private RowScope<E, C> rows = RowScope.all();
        private final Set<String> read = new LinkedHashSet<>();
        private final Map<String, Mask> masked = new LinkedHashMap<>();
        private final Set<String> create = new LinkedHashSet<>();
        private final Set<String> update = new LinkedHashSet<>();
        private boolean delete;

        Builder(EntityDescriptor<E, C> entity) {
            this.entity = entity;
        }

        /** The rows the grant reaches; every row when not declared. */
        public Builder<E, C> rows(RowScope<E, C> rows) {
            this.rows = rows;
            return this;
        }

        /** Fields read as they are — and usable in filters and orderings. */
        public Builder<E, C> read(E projection) {
            read.addAll(entity.fieldsSetIn(projection));
            return this;
        }

        /** Fields read through {@code mask}; only textual fields, and never the key. */
        public Builder<E, C> mask(E projection, Mask mask) {
            if (entity.key().isSet(projection)) {
                throw new IllegalStateException(entity.name() + "." + entity.keyName()
                        + " is the key, always read as it is, and cannot be masked");
            }
            for (var fieldName : entity.fieldsSetIn(projection)) {
                if (!entity.field(fieldName).textual()) {
                    throw new IllegalStateException(entity.name() + "." + fieldName + " is not textual and cannot be masked");
                }
                masked.put(fieldName, mask);
            }
            return this;
        }

        /** Fields that may be set when creating a row. */
        public Builder<E, C> create(E projection) {
            create.addAll(entity.fieldsSetIn(projection));
            return this;
        }

        /** Fields that may be changed in a reachable row. */
        public Builder<E, C> update(E projection) {
            update.addAll(entity.fieldsSetIn(projection));
            return this;
        }

        /** The reachable rows may be deleted. */
        public Builder<E, C> delete() {
            this.delete = true;
            return this;
        }

        DataGrant<E, C> build() {
            for (var fieldName : masked.keySet()) {
                if (read.contains(fieldName)) {
                    throw new IllegalStateException(entity.name() + "." + fieldName + " is declared both read and masked");
                }
                if (create.contains(fieldName) || update.contains(fieldName)) {
                    throw new IllegalStateException(entity.name() + "." + fieldName
                            + " is masked and cannot be written by the same grant");
                }
            }
            return new DataGrant<>(this);
        }
    }

    static <E, C extends Criteria> Builder<E, C> builder(EntityDescriptor<E, C> entity) {
        return new Builder<>(entity);
    }
}
