package br.com.wdc.framework.domain.security.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import br.com.wdc.framework.domain.criteria.Criteria;
import br.com.wdc.framework.domain.criteria.Criterion;
import br.com.wdc.framework.domain.projection.ProjectionList;

/**
 * What a {@link DataPolicy} needs to know about an entity: its fields, by the names they have on the wire, its
 * relations to other entities, and how its criteria refer to them.
 *
 * <p>
 * A policy is declared with projections — {@code new User().withName(pv.str)} — and the descriptor is what turns a
 * projection into the set of fields it names, and back. It also says which field each criteria field filters and which
 * fields each ordering sorts by, because filtering or sorting by a field reveals its values as much as reading it does.
 * </p>
 *
 * <p>
 * A field is a <b>value</b>, a <b>relation</b> to one row of another entity ({@code purchase.user}), or a
 * <b>collection</b> of rows of another entity ({@code purchase.items}). A relation or collection names the policy of
 * the other entity: what is read inside it goes through that policy, so an entity has one set of rules wherever it
 * appears. The policy is given as a {@link Supplier} because entities refer to each other — the purchase to its items,
 * the item to its purchase.
 * </p>
 *
 * <p>
 * Built once per entity, next to its policy. The names are the ones the entity codec writes, so the fields a
 * {@link DataAccess} refuses are named as the client sent them.
 * </p>
 *
 * @param <E> the entity
 * @param <C> its criteria
 */
public final class EntityDescriptor<E, C extends Criteria> {

    /** What a field holds. */
    enum Kind {
        VALUE, RELATION, COLLECTION
    }

    /**
     * A field of the entity: its name, how to read and write it, and how it is marked in a projection.
     *
     * @param <E> the entity
     * @param <T> the field type
     */
    public static final class Field<E, T> {

        private final String name;
        private final Kind kind;
        private final Function<E, T> getter;
        private final BiConsumer<E, T> setter;
        private final T marker;
        private final Supplier<? extends DataPolicy<?, ?>> target;
        private final String inverse;

        Field(String name, Kind kind, Function<E, T> getter, BiConsumer<E, T> setter, T marker,
                Supplier<? extends DataPolicy<?, ?>> target, String inverse) {
            this.name = name;
            this.kind = kind;
            this.getter = getter;
            this.setter = setter;
            this.marker = marker;
            this.target = target;
            this.inverse = inverse;
        }

        public String name() {
            return name;
        }

        Kind kind() {
            return kind;
        }

        /** Whether the field is textual — only a textual field can be masked. */
        public boolean textual() {
            return kind == Kind.VALUE && marker instanceof String;
        }

        /** The policy of the related entity — for a relation or a collection. */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        DataPolicy<Object, Criteria> targetPolicy() {
            return (DataPolicy) target.get();
        }

        /** The descriptor of the related entity — for a relation or a collection. */
        EntityDescriptor<Object, Criteria> targetEntity() {
            return targetPolicy().entity();
        }

        /** For a collection: the field of each child that points back to the owner. */
        String inverse() {
            return inverse;
        }

        T get(E entity) {
            return getter.apply(entity);
        }

        boolean isSet(E entity) {
            return getter.apply(entity) != null;
        }

        void clear(E entity) {
            setter.accept(entity, null);
        }

        @SuppressWarnings("unchecked")
        void setRaw(E entity, Object value) {
            setter.accept(entity, (T) value);
        }

        /** Marks the field in a projection: the marker of a value; the key of a relation or of the collection's rows. */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        void mark(E projection) {
            switch (kind) {
            case VALUE -> setter.accept(projection, marker);
            case RELATION -> setter.accept(projection, (T) targetEntity().projectionOf(Set.of()));
            case COLLECTION -> setter.accept(projection, (T) new ProjectionList(targetEntity().projectionOf(Set.of()), null));
            }
        }

        void copy(E from, E to) {
            setter.accept(to, getter.apply(from));
        }

        @SuppressWarnings("unchecked")
        void mask(E entity, Mask mask) {
            var value = getter.apply(entity);
            if (value != null) {
                setter.accept(entity, (T) mask.apply((String) value));
            }
        }
    }

    private final String name;
    private final Supplier<E> entityFactory;
    private final Supplier<C> criteriaFactory;
    private final Function<C, E> projectionGetter;
    private final BiConsumer<C, E> projectionSetter;
    private final Function<C, List<C>> anyOfGetter;
    private final BiConsumer<C, List<C>> anyOfSetter;
    private final Field<E, ?> key;
    private final Map<String, Field<E, ?>> fields;
    private final Map<String, String> criterionFields;
    private final Function<C, Set<String>> orderingFields;
    private final Supplier<E> defaultShape;

    private EntityDescriptor(Builder<E, C> b) {
        this.name = b.name;
        this.entityFactory = b.entityFactory;
        this.criteriaFactory = b.criteriaFactory;
        this.projectionGetter = b.projectionGetter;
        this.projectionSetter = b.projectionSetter;
        this.anyOfGetter = b.anyOfGetter;
        this.anyOfSetter = b.anyOfSetter;
        this.key = b.key;
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(b.fields));
        this.criterionFields = Collections.unmodifiableMap(new HashMap<>(b.criterionFields));
        this.orderingFields = b.orderingFields;
        this.defaultShape = b.defaultShape;
    }

    public static <E, C extends Criteria> Builder<E, C> builder(String name) {
        return new Builder<>(name);
    }

    /** The entity name used in the messages, as in the REST paths ({@code user}, {@code purchase-item}). */
    public String name() {
        return name;
    }

    /** The key field. It identifies the row, so every grant that reads the entity reads it too. */
    public String keyName() {
        return key.name();
    }

    /** The fields other than the key, in declaration order. */
    public Set<String> fieldNames() {
        var names = new LinkedHashSet<>(fields.keySet());
        names.remove(key.name());
        return names;
    }

    Field<E, ?> field(String fieldName) {
        var field = fields.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException(name + " has no field " + fieldName);
        }
        return field;
    }

    Field<E, ?> key() {
        return key;
    }

    /**
     * The fields, other than the key, that are set in {@code entity} — for a projection, the fields it asks for. A
     * relation or a collection counts when it is present, whatever it carries inside.
     */
    Set<String> fieldsSetIn(E entity) {
        var names = new LinkedHashSet<String>();
        if (entity == null) {
            return names;
        }
        for (var field : fields.values()) {
            if (field != key && field.isSet(entity)) {
                names.add(field.name());
            }
        }
        return names;
    }

    /** A projection with the key and the given fields — relations and collections by key only. */
    E projectionOf(Set<String> fieldNames) {
        var projection = entityFactory.get();
        key.mark(projection);
        for (var fieldName : fieldNames) {
            field(fieldName).mark(projection);
        }
        return projection;
    }

    /**
     * Adds to {@code into} what {@code from} asks for, going into the relations. It is how the fields a row scope reads
     * join the projection of a read. A collection cannot be merged: a row scope does not reach through one.
     */
    void merge(E into, E from) {
        if (from == null) {
            return;
        }
        for (var fieldName : fieldsSetIn(from)) {
            var field = field(fieldName);
            switch (field.kind()) {
            case VALUE -> field.mark(into);
            case RELATION -> {
                if (!field.isSet(into)) {
                    field.mark(into);
                }
                field.targetEntity().merge(field.get(into), field.get(from));
            }
            case COLLECTION -> throw new IllegalStateException(
                    name + "." + fieldName + " is a collection, and a row scope cannot read through it");
            }
        }
    }

    /** Clears every field of {@code row} but the key — a related row read by its key only. */
    void keepKeyOnly(E row) {
        for (var field : fields.values()) {
            if (field != key) {
                field.clear(row);
            }
        }
    }

    E newEntity() {
        return entityFactory.get();
    }

    /**
     * What a read without a projection gets, before it is trimmed to what the requester may read: the shape the entity
     * declares, or else every field but the collections, relations by key.
     */
    E defaultShape() {
        if (defaultShape != null) {
            return defaultShape.get();
        }
        var shallow = new LinkedHashSet<String>();
        for (var field : fields.values()) {
            if (field != key && field.kind() != Kind.COLLECTION) {
                shallow.add(field.name());
            }
        }
        return projectionOf(shallow);
    }

    C newCriteria() {
        return criteriaFactory.get();
    }

    E projection(C criteria) {
        return projectionGetter.apply(criteria);
    }

    void setProjection(C criteria, E projection) {
        projectionSetter.accept(criteria, projection);
    }

    List<C> anyOf(C criteria) {
        return anyOfGetter.apply(criteria);
    }

    void setAnyOf(C criteria, List<C> groups) {
        anyOfSetter.accept(criteria, groups);
    }

    /**
     * The fields, other than the key, that the criteria filters or sorts by — its own fields, those of its alternative
     * groups, and those of its ordering.
     */
    Set<String> filteredFields(C criteria) {
        var names = new LinkedHashSet<String>();
        collectFiltered(criteria, names);
        names.addAll(orderingFields.apply(criteria));
        names.remove(key.name());
        return names;
    }

    private void collectFiltered(C criteria, Set<String> names) {
        for (Criterion<?, ?> criterion : criteria.criterions()) {
            if (criterion.isSet()) {
                names.add(criterionFields.getOrDefault(criterion.name(), criterion.name()));
            }
        }
        for (var group : anyOf(criteria)) {
            collectFiltered(group, names);
        }
    }

    /**
     * Builds the descriptor. {@link #build()} checks that every criteria field resolves to an entity field, so a
     * criteria field added without a mapping fails when the policy is loaded, not when a request uses it.
     */
    public static final class Builder<E, C extends Criteria> {

        private final String name;
        private Supplier<E> entityFactory;
        private Supplier<C> criteriaFactory;
        private Function<C, E> projectionGetter;
        private BiConsumer<C, E> projectionSetter;
        private Function<C, List<C>> anyOfGetter;
        private BiConsumer<C, List<C>> anyOfSetter;
        private Field<E, ?> key;
        private final Map<String, Field<E, ?>> fields = new LinkedHashMap<>();
        private final Map<String, String> criterionFields = new HashMap<>();
        private Function<C, Set<String>> orderingFields = c -> Set.of();
        private Supplier<E> defaultShape;

        private Builder(String name) {
            this.name = name;
        }

        public Builder<E, C> entity(Supplier<E> factory) {
            this.entityFactory = factory;
            return this;
        }

        public Builder<E, C> criteria(Supplier<C> factory) {
            this.criteriaFactory = factory;
            return this;
        }

        public Builder<E, C> projection(Function<C, E> getter, BiConsumer<C, E> setter) {
            this.projectionGetter = getter;
            this.projectionSetter = setter;
            return this;
        }

        public Builder<E, C> anyOf(Function<C, List<C>> getter, BiConsumer<C, List<C>> setter) {
            this.anyOfGetter = getter;
            this.anyOfSetter = setter;
            return this;
        }

        public <T> Builder<E, C> key(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, T marker) {
            this.key = new Field<>(fieldName, Kind.VALUE, getter, setter, marker, null, null);
            fields.put(fieldName, key);
            return this;
        }

        public <T> Builder<E, C> field(String fieldName, Function<E, T> getter, BiConsumer<E, T> setter, T marker) {
            fields.put(fieldName, new Field<>(fieldName, Kind.VALUE, getter, setter, marker, null, null));
            return this;
        }

        /** A relation to one row of another entity, read through that entity's policy. */
        public <R> Builder<E, C> relation(String fieldName, Function<E, R> getter, BiConsumer<E, R> setter,
                Supplier<? extends DataPolicy<R, ?>> target) {
            fields.put(fieldName, new Field<>(fieldName, Kind.RELATION, getter, setter, null, target, null));
            return this;
        }

        /**
         * A collection of rows of another entity, read and created through that entity's policy.
         *
         * @param inverse the relation of each child that points back to the owner — set on the children of a new row
         *        before they are checked, since a child of a new row belongs to it
         */
        public <R> Builder<E, C> collection(String fieldName, Function<E, List<R>> getter,
                BiConsumer<E, List<R>> setter, Supplier<? extends DataPolicy<R, ?>> target, String inverse) {
            fields.put(fieldName, new Field<>(fieldName, Kind.COLLECTION, getter, setter, null, target, inverse));
            return this;
        }

        /** A criteria field whose name differs from the entity field it filters ({@code userId} → {@code id}). */
        public Builder<E, C> criterion(String criterionName, String fieldName) {
            criterionFields.put(criterionName, fieldName);
            return this;
        }

        /** The fields each ordering of the criteria sorts by. */
        public Builder<E, C> orderings(Function<C, Set<String>> orderingFields) {
            this.orderingFields = orderingFields;
            return this;
        }

        /**
         * The shape a read without a projection gets — it may go into relations and collections, as a receipt does with
         * its items. A new projection on each call: a read changes the one it is given. Without it, every field but the
         * collections, relations by key.
         */
        public Builder<E, C> defaultShape(Supplier<E> shape) {
            this.defaultShape = shape;
            return this;
        }

        public EntityDescriptor<E, C> build() {
            if (entityFactory == null || criteriaFactory == null || projectionGetter == null || anyOfGetter == null
                    || key == null) {
                throw new IllegalStateException(name + ": entity, criteria, projection, anyOf and key are required");
            }
            var unknown = new ArrayList<String>();
            for (Criterion<?, ?> criterion : criteriaFactory.get().criterions()) {
                var fieldName = criterionFields.getOrDefault(criterion.name(), criterion.name());
                if (!fields.containsKey(fieldName)) {
                    unknown.add(criterion.name());
                }
            }
            if (!unknown.isEmpty()) {
                throw new IllegalStateException(name + ": criteria fields with no entity field: " + unknown);
            }
            return new EntityDescriptor<>(this);
        }
    }
}
