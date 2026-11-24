package br.com.wdc.framework.jooq;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import org.jooq.SelectQuery;
import org.jooq.SortField;
import org.jooq.Table;
import org.jooq.impl.DSL;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;
import br.com.wdc.framework.commons.function.ThrowingConsumer;

/**
 * Declarative query builder for bean ↔ jOOQ table mapping with JSON projection.
 *
 * <p>
 * <b>Projection</b> support: a bean with non-null fields indicates which columns to include in the SELECT.
 * <b>Lazy</b> support: registration of child collections is deferred until the first execution.
 * Collections (1:N) via correlated subselect with a JSON array.
 * SELECTs only — no insert/update support.
 * </p>
 *
 * <p>
 * Typical usage (in the repository):
 *
 * <pre>{@code
 * private static final JsonQuery<User, TUser> QUERY =
 *     new JsonQueryBuilder<User, TUser>()
 *         .setAlias("u")
 *         .setBeanFactory(User::new)
 *         .setTableFactory(alias -> USER.as(alias))
 *         .addI64("id", u -> u.id, (u, v) -> u.id = v, t -> t.ID)
 *         .addStr("login", u -> u.login, (u, v) -> u.login = v, t -> t.LOGIN)
 *         .build();
 *
 * // Full projection:
 * User prj = QUERY.newProjectionBean();
 * List<User> list = QUERY.fetchToList(prj, (t, q) -> q.where(t.STATUS.eq("ACTIVE")));
 *
 * // Partial projection (only id and login):
 * User prj = QUERY.newProjectionBean(u -> { u.name = null; u.profile = null; ... });
 * }</pre>
 *
 * @param <B> domain bean type
 * @param <T> schema class type (e.g. TUser)
 */
@SuppressWarnings("java:S4276") // getters use boxed types as projection sentinels (null = field excluded from the SELECT)
public class JsonQueryBuilder<B, T extends Table<?>> {

    // :: Functional callback for projection (chain of responsibility)

    @FunctionalInterface
    private interface FieldProjectionCallback<B, T> {

        @SuppressWarnings("unchecked")
        static <B, T> FieldProjectionCallback<B, T> noop() {
            return (FieldProjectionCallback<B, T>) NOOP;
        }

        FieldProjectionCallback<?, ?> NOOP = (f, c, b, t) -> {
        };

        void accept(List<JsonFieldEntry> fields, QueryContext ctx, B bean, T table);

        default FieldProjectionCallback<B, T> andThen(FieldProjectionCallback<? super B, ? super T> after) {
            Objects.requireNonNull(after);
            return (fields, ctx, bean, table) -> {
                accept(fields, ctx, bean, table);
                after.accept(fields, ctx, bean, table);
            };
        }
    }

    // :: Functional callback for JSON parsing

    @FunctionalInterface
    private interface FieldSetterCallback<B> {
        void accept(B bean, JsonReader reader) throws IOException;
    }

    // :: Builder fields

    private String tableName;
    private Supplier<B> beanFactory;
    private Function<String, T> tableFactory;
    private Supplier<DSLContext> dslContextSupplier;
    private Consumer<B> fillProjBean;
    private FieldProjectionCallback<B, T> fieldPrjList;
    private Map<String, FieldSetterCallback<B>> fieldSetterMap;
    /**
     * How to ask a projection bean whether each field was requested.
     *
     * <p>
     * Exists for the foreign key shortcut: this is how {@code projectsBeyond} finds out whether the association's
     * projection requests anything beyond the key. Covers every registered field — scalar or relation.
     * </p>
     */
    private final Map<String, Function<B, Object>> fieldPresenceMap = new LinkedHashMap<>();
    /** Translation of this entity's criteria {@code OrderBy}; {@code null} when the entity did not register one. */
    private BiFunction<T, Object, List<SortField<?>>> ordering;
    private Consumer<JsonQueryBuilder<B, T>> lazyInit; // guarded by lazyLock during initialization
    private final ReentrantLock lazyLock = new ReentrantLock();
    private volatile boolean lazyDone = false;

    public JsonQueryBuilder() {
        this.fieldSetterMap = new HashMap<>();
        this.lazyInit = ThrowingConsumer.noop();
        this.fieldPrjList = FieldProjectionCallback.noop();
        this.fillProjBean = ThrowingConsumer.noop();
    }

    // :: Configuration

    /** Base alias for the main table (e.g. "u" for user). */
    public JsonQueryBuilder<B, T> setAlias(String name) {
        this.tableName = name;
        return this;
    }

    /** Domain bean factory (e.g. {@code User::new}). */
    public JsonQueryBuilder<B, T> setBeanFactory(Supplier<B> beanFactory) {
        this.beanFactory = beanFactory;
        return this;
    }

    /** Aliased schema table factory (e.g. {@code alias -> USER.as(alias)}). */
    public JsonQueryBuilder<B, T> setTableFactory(Function<String, T> tableFactory) {
        this.tableFactory = tableFactory;
        return this;
    }

    /**
     * How to translate this entity's criteria {@code OrderBy} into {@code ORDER BY}.
     *
     * <p>
     * The translation is per entity and lives in the repository — it is the one that knows the constants. Registered
     * here, it lets a child collection be ordered with the same vocabulary the root query already uses, without
     * inventing a second one.
     * </p>
     *
     * <p>
     * Receives the criteria as {@code Object} because the child collection carries the criteria of <b>its own</b>
     * entity, which the caller does not know statically; the function checks the type and returns an empty list when
     * it does not recognize it.
     * </p>
     */
    public JsonQueryBuilder<B, T> setOrdering(BiFunction<T, Object, List<SortField<?>>> ordering) {
        this.ordering = ordering;
        return this;
    }

    /**
     * Supplier of the {@link DSLContext} used by the queries. Each application injects its own (lazy resolution, per
     * execution), keeping {@code framework.jooq} free of any global {@code DSLContext} holder.
     */
    public JsonQueryBuilder<B, T> setDSLContextSupplier(Supplier<DSLContext> dslContextSupplier) {
        this.dslContextSupplier = dslContextSupplier;
        return this;
    }

    /**
     * The table's primary key, resolved on the given instance — which has its own alias inside a subselect.
     *
     * <p>
     * Comes from the table generated by jOOQ, not from a declaration: the schema already knows it. Serves the slicing
     * of a child collection, which needs something to say "these rows, and not the others".
     * </p>
     *
     * @throws IllegalStateException if the table declares no primary key, or if it is composite — a single-column
     *         {@code IN} cannot express it, and emitting approximate SQL would silently return the wrong collection
     */
    @SuppressWarnings("unchecked")
    private static Field<Object> primaryKeyOf(Table<?> table) {
        var pk = table.getPrimaryKey();
        if (pk == null || pk.getFields().isEmpty()) {
            throw new IllegalStateException("collection slicing requires a primary key, and " + table.getName()
                    + " declares none");
        }
        if (pk.getFields().size() > 1) {
            throw new IllegalStateException("collection slicing does not support a composite primary key: "
                    + table.getName() + " has " + pk.getFields().size() + " key columns");
        }
        var column = pk.getFields().get(0);
        var resolved = table.field(column);
        return (Field<Object>) (resolved != null ? resolved : column);
    }

    /**
     * The JSON entry of a child collection (1:N), with the ordering and slice the collection itself declares.
     *
     * <p>
     * Shared by {@code addBeanSetField} and {@code addBeanListField}: what differs between the two is the collection
     * type, not how the subquery is built. Returns {@code null} when there is nothing to project.
     * </p>
     */
    private <C, U extends Table<?>> JsonFieldEntry childCollectionEntry(String fn, Collection<C> childPrjBeans,
            QueryContext ctx, T table, JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        if (childPrjBeans == null || childPrjBeans.isEmpty()) {
            return null;
        }
        var childPrjBean = childPrjBeans.iterator().next();
        if (childPrjBean == null) {
            return null;
        }

        var childQueryBuilder = new JsonChildQueryBuilder<T, U>(ctx, table);
        if (childPrjBeans instanceof HasCriteria hasCriteria) {
            childQueryBuilder.criteria = hasCriteria.getCriteria();
        }
        var clause = (BiConsumer<U, SelectJoinStep<Record1<String>>>) (tbChild, q) -> {
            childQueryBuilder.childTable = tbChild;
            childQueryBuilder.dsl = q;
            childWhereClause.accept(childQueryBuilder);
        };

        // Ordering and slice come from what the collection itself declares: the criteria it carries (HasCriteria)
        // bring the child entity's OrderBy, and HasSlice brings the limit and offset. With neither, the query is the
        // plain aggregation.
        final var listCriteria = childQueryBuilder.criteria;
        Function<U, List<SortField<?>>> order = null;
        if (listCriteria != null && childQuery.hasOrdering()) {
            order = tb -> childQuery.orderingOf(tb, listCriteria);
        }
        Integer limit = null;
        Integer offset = null;
        if (childPrjBeans instanceof HasSlice slice) {
            limit = slice.getLimit();
            offset = slice.getOffset();
        }

        return new JsonFieldEntry(fn,
                DSL.field(childQuery.selectOrdered(ctx, childPrjBean, clause, order, limit, offset)),
                JsonFieldType.RAW_JSON);
    }

    private DSLContext requireDslContext() {
        if (dslContextSupplier == null) {
            throw new IllegalStateException("JsonQueryBuilder: setDSLContextSupplier(...) was not configured");
        }
        return dslContextSupplier.get();
    }



    // :: Scalar field registration

    /** Long field (BIGINT). */
    public JsonQueryBuilder<B, T> addI64(String fn, Function<B, Long> getter,
            BiConsumer<B, Long> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0L));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.NUMBER));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.NUMBER) {
                setter.accept(bean, reader.nextLong());
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** Integer field (INT). */
    public JsonQueryBuilder<B, T> addI32(String fn, Function<B, Integer> getter,
            BiConsumer<B, Integer> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.NUMBER));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.NUMBER) {
                setter.accept(bean, reader.nextInt());
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /**
     * Scalar Enum field (VARCHAR in the database, Java enum in the application).
     *
     * <p>
     * The {@code parser} must be the enum's own lookup method (e.g. {@code Role::parse}). It must return
     * {@code null} for unknown or null values — never throw.
     *
     * @param enumClass enum class; used to obtain the projection sentinel via {@code getEnumConstants()[0]}
     * @param parser    deserialization function String → E (e.g. {@code Role::parse})
     */
    public <E extends Enum<E>> JsonQueryBuilder<B, T> addEnm(String fn,
            Function<B, E> getter,
            BiConsumer<B, E> setter,
            Class<E> enumClass,
            Function<String, E> parser,
            Function<T, Field<String>> jooqField) {

        E sentinel = enumClass.getEnumConstants()[0];

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, sentinel));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.STRING));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() != JsonToken.NULL) {
                setter.accept(bean, parser.apply(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** String field (VARCHAR). */
    public JsonQueryBuilder<B, T> addStr(String fn, Function<B, String> getter,
            BiConsumer<B, String> setter, Function<T, Field<String>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, ""));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.STRING));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() != JsonToken.NULL) {
                setter.accept(bean, reader.nextString());
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** Boolean field. */
    public JsonQueryBuilder<B, T> addBit(String fn, Function<B, Boolean> getter,
            BiConsumer<B, Boolean> setter, Function<T, Field<Boolean>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, Boolean.FALSE));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.BOOLEAN));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.BOOLEAN) {
                setter.accept(bean, reader.nextBoolean());
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** OffsetDateTime field (TIMESTAMPTZ). */
    public JsonQueryBuilder<B, T> addOdt(String fn, Function<B, OffsetDateTime> getter,
            BiConsumer<B, OffsetDateTime> setter, Function<T, Field<OffsetDateTime>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, OffsetDateTime.MIN));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.DATETIME));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.STRING) {
                setter.accept(bean, OffsetDateTime.parse(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /**
     * LocalDateTime field (TIMESTAMP without time zone) mapped to OffsetDateTime in the domain.
     *
     * <p>
     * Useful when the database stores TIMESTAMP (without TZ) but the domain uses {@link OffsetDateTime}. Reading applies
     * a lenient parse that accepts both full ISO-8601 and the H2 format ({@code yyyy-MM-dd HH:mm:ss[.n]}).
     * </p>
     */
    public JsonQueryBuilder<B, T> addLdt(String fn, Function<B, OffsetDateTime> getter,
            BiConsumer<B, OffsetDateTime> setter, Function<T, Field<java.time.LocalDateTime>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, OffsetDateTime.MIN));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.DATETIME));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.STRING) {
                setter.accept(bean, parseLdt(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /**
     * The value to write to a TIMESTAMP column without time zone read by {@link #addLdt}: the same instant, in UTC —
     * reading interprets the column as UTC. {@code null} for {@code null}.
     */
    public static java.time.LocalDateTime toUtcLdt(OffsetDateTime value) {
        return value != null ? value.withOffsetSameInstant(java.time.ZoneOffset.UTC).toLocalDateTime() : null;
    }

    private static OffsetDateTime parseLdt(String s) {
        try {
            return OffsetDateTime.parse(s);
        } catch (java.time.format.DateTimeParseException ex) {
            // H2 format: "2024-01-15 10:30:00" or "2024-01-15 10:30:00.123456"
            var ldt = java.time.LocalDateTime.parse(s.replace(' ', 'T'));
            return ldt.atOffset(java.time.ZoneOffset.UTC);
        }
    }

    /** Double field (DOUBLE PRECISION / NUMERIC). */
    public JsonQueryBuilder<B, T> addF64(String fn, Function<B, Double> getter,
            BiConsumer<B, Double> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0.0));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.NUMBER));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.NUMBER) {
                setter.accept(bean, reader.nextDouble());
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** BigDecimal field (DECIMAL / NUMERIC). */
    public JsonQueryBuilder<B, T> addDec(String fn, Function<B, java.math.BigDecimal> getter,
            BiConsumer<B, java.math.BigDecimal> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, java.math.BigDecimal.ZERO));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.NUMBER));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.NUMBER) {
                setter.accept(bean, new java.math.BigDecimal(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /** byte[] field (BINARY / BLOB). Serialized as Base64 in the JSON. */
    public JsonQueryBuilder<B, T> addBin(String fn, Function<B, byte[]> getter,
            BiConsumer<B, byte[]> setter, Function<T, Field<byte[]>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, new byte[0]));

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(new JsonFieldEntry(fn, jooqField.apply(table), JsonFieldType.BINARY));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.STRING) {
                setter.accept(bean, java.util.Base64.getMimeDecoder().decode(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    // :: Lazy (defer child query registration)

    /**
     * Defers field registration (typically child collections) until the first execution. Avoids circular dependencies
     * and expensive initialization.
     */
    public JsonQueryBuilder<B, T> lazy(Consumer<JsonQueryBuilder<B, T>> initCallback) {
        var prev = this.lazyInit;
        this.lazyInit = me -> {
            prev.accept(me);
            initCallback.accept(me);
        };
        return this;
    }

    /** Runs the lazy initialization exactly once, thread-safe via double-checked locking with ReentrantLock. */
    void runLazyInit() {
        if (!lazyDone) {
            lazyLock.lock();
            try {
                if (!lazyDone) {
                    lazyInit.accept(this);
                    lazyDone = true;
                }
            } finally {
                lazyLock.unlock();
            }
        }
    }

    // :: Child collections (1:N) — correlated subselect

    /**
     * Registers a Set&lt;ChildBean&gt; field loaded via correlated subselect.
     *
     * @param fn         JSON name of the field
     * @param getter     getter of the Set on the parent bean
     * @param setter     setter of the Set on the parent bean
     * @param childQuery JsonQuery built for the child bean
     * @param childWhere callback that defines the correlation (WHERE child.fk = parent.pk)
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanSetField(String fn,
            Function<B, Set<C>> getter,
            BiConsumer<B, Set<C>> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var entry = childCollectionEntry(fn, getter.apply(bean), ctx, table, childQuery, childWhereClause);
            if (entry != null) {
                fields.add(entry);
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                var beanSet = new LinkedHashSet<C>();
                reader.beginArray();
                while (reader.hasNext()) {
                    C childBean = childQuery.parseJson(reader);
                    if (childBean != null) {
                        beanSet.add(childBean);
                    }
                }
                reader.endArray();
                setter.accept(bean, beanSet);
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /**
     * Registers a List&lt;ChildBean&gt; field loaded via correlated subselect.
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanListField(String fn,
            Function<B, List<C>> getter,
            BiConsumer<B, List<C>> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        this.fieldPresenceMap.put(fn, getter::apply);

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var entry = childCollectionEntry(fn, getter.apply(bean), ctx, table, childQuery, childWhereClause);
            if (entry != null) {
                fields.add(entry);
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.BEGIN_ARRAY) {
                var beanList = new ArrayList<C>();
                reader.beginArray();
                while (reader.hasNext()) {
                    var childBean = childQuery.parseJson(reader);
                    if (childBean != null) {
                        beanList.add(childBean);
                    }
                }
                reader.endArray();
                setter.accept(bean, beanList);
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    /**
     * Registers a ChildBean field (1:1 relation) loaded via subselect.
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanField(String fn,
            Function<B, C> getter,
            BiConsumer<B, C> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        return addBeanField(fn, getter, setter, childQuery, childWhereClause, null);
    }

    /**
     * Same as above, declaring <b>which column of this table holds the key</b> of the associated entity.
     *
     * <p>
     * With the key declared, a projection that requests nothing beyond it is built from the row itself, and the
     * subselect does not happen — the common case of {@code newProjection()}, which brings the association only to
     * carry the id. Without the key, or when the projection requests any other field, the relation is resolved through
     * the subselect.
     * </p>
     *
     * <p>
     * <b>The names declared in the key are those of the child's JSON</b>, not those of this table: that is how the
     * reading on the other side recognizes the object that comes out of here.
     * </p>
     *
     * @param key declares the child-field → this-table-column mapping; {@code null} disables the shortcut
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanField(String fn,
            Function<B, C> getter,
            BiConsumer<B, C> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause,
            Consumer<RelationKey<T>> key) {

        this.fieldPresenceMap.put(fn, getter::apply);

        final RelationKey<T> relationKey;
        if (key == null) {
            relationKey = null;
        } else {
            relationKey = new RelationKey<>();
            key.accept(relationKey);
        }

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var childPrjBean = getter.apply(bean);
            if (childPrjBean == null) {
                return;
            }

            // The key is already in this row: if the associated projection requests nothing else, building it here
            // avoids a subselect that would only rediscover the foreign key column.
            if (relationKey != null && !relationKey.isEmpty()
                    && !childQuery.projectsBeyond(childPrjBean, relationKey.names())) {
                var dialect = JsonDialect.of(ctx.dsl().dialect());
                fields.add(new JsonFieldEntry(fn,
                        dialect.jsonObject(relationKey.entries(table)), JsonFieldType.RAW_JSON));
                return;
            }

            var childQueryBuilder = new JsonChildQueryBuilder<T, U>(ctx, table);
            var clause = (BiConsumer<U, SelectJoinStep<Record1<String>>>) (tbChild, q) -> {
                childQueryBuilder.childTable = tbChild;
                childQueryBuilder.dsl = q;
                childWhereClause.accept(childQueryBuilder);
            };

            fields.add(new JsonFieldEntry(fn,
                    DSL.field(childQuery.select(ctx, childPrjBean, clause, false)), JsonFieldType.RAW_JSON));
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.BEGIN_OBJECT) {
                var childBean = childQuery.parseJson(reader);
                setter.accept(bean, childBean);
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    // :: Build

    /**
     * Builds the immutable query instance, ready for execution.
     */
    public JsonQuery<B, T> build() {
        final JsonQueryBuilder<B, T> me = this;

        return new JsonQuery<>() {

            @Override
            public B newBean() {
                return me.beanFactory.get();
            }

            @Override
            public B newProjectionBean() {
                B bean = me.beanFactory.get();
                me.fillProjBean.accept(bean);
                return bean;
            }

            @Override
            public B newProjectionBean(Consumer<B> adapt) {
                B bean = me.beanFactory.get();
                me.fillProjBean.accept(bean);
                adapt.accept(bean);
                return bean;
            }

            @Override
            public T newTable(String alias) {
                return me.tableFactory.apply(alias);
            }

            @Override
            public SelectQuery<Record1<String>> select(B prjBean,
                    BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause) {
                var ctx = new QueryContext(me.requireDslContext());
                return select(ctx, prjBean, whereClause, false);
            }

            @Override
            public SelectQuery<Record1<String>> select(QueryContext ctx, B prjBean,
                    BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause, boolean isAgg) {
                me.runLazyInit();

                var tbRoot = me.tableFactory.apply(me.tableName + ctx.nextUniqueInt());
                var dialect = JsonDialect.of(ctx.dsl().dialect());

                var prjField = isAgg
                        ? dialect.jsonArrayAgg(projection(ctx, tbRoot, prjBean))
                        : projection(ctx, tbRoot, prjBean);

                var joinStep = ctx.dsl()
                        .select(prjField.as(me.tableName + "_json"))
                        .from(tbRoot);

                whereClause.accept(tbRoot, joinStep);
                return joinStep.getQuery();
            }

            @Override
            public Field<String> projection(QueryContext ctx, T jooqTable, B prjBean) {
                me.runLazyInit();
                var entries = new ArrayList<JsonFieldEntry>();
                me.fieldPrjList.accept(entries, ctx,
                        prjBean != null ? prjBean : this.newProjectionBean(), jooqTable);
                var dialect = JsonDialect.of(ctx.dsl().dialect());
                return dialect.jsonObject(entries);
            }

            @Override
            public boolean hasOrdering() {
                return me.ordering != null;
            }

            @Override
            public List<SortField<?>> orderingOf(T table, Object criteria) {
                if (me.ordering == null) {
                    return List.of();
                }
                var sort = me.ordering.apply(table, criteria);
                return sort != null ? sort : List.of();
            }

            @Override
            public SelectQuery<Record1<String>> selectOrdered(QueryContext ctx, B prjBean,
                    BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause,
                    Function<T, List<SortField<?>>> order, Integer limit, Integer offset) {
                me.runLazyInit();

                if (order == null && limit == null && offset == null) {
                    // Without ordering or slice, the plain aggregation: one query fewer, and whoever requests
                    // nothing gets the simplest plan.
                    return select(ctx, prjBean, whereClause, true);
                }

                var tbRoot = me.tableFactory.apply(me.tableName + ctx.nextUniqueInt());
                var dialect = JsonDialect.of(ctx.dsl().dialect());

                var sort = order != null ? order.apply(tbRoot) : null;
                if (sort != null && !sort.isEmpty() && !dialect.supportsOrderedAggregation()) {
                    // Refuses instead of returning the collection out of the requested order: a wrongly ordered
                    // result passes for correct, and the error only shows up far from here.
                    throw new UnsupportedOperationException("the dialect " + ctx.dsl().dialect()
                            + " cannot order inside the aggregation; order the collection on the application side");
                }

                // The ordering goes INSIDE the aggregation. Wrapping this query in a derived table — where a plain
                // ORDER BY would fit — would put the correlation with the parent row out of reach: a derived table
                // does not see the outer scope, and the database answers "column parent.id not found".
                var agg = ctx.dsl()
                        .select(dialect.jsonArrayAgg(projection(ctx, tbRoot, prjBean), sort)
                                .as(me.tableName + "_json"))
                        .from(tbRoot);
                whereClause.accept(tbRoot, agg);
                var aggQuery = agg.getQuery();

                // The slice does not fit in the aggregation, and for the same reason it does not fit in a derived
                // table. It comes out as an IN over the primary key: a correlated subquery — this one does see the
                // parent — that repeats the same filter, orders and cuts, returning the keys of the rows that stay.
                if (limit != null || offset != null) {
                    var tbSlice = me.tableFactory.apply(me.tableName + ctx.nextUniqueInt());
                    var keep = ctx.dsl().select(primaryKeyOf(tbSlice)).from(tbSlice);

                    // The whereClause is typed on the projection's Record because that is how the rest of the API
                    // uses it; here it only adds conditions, and the projected column's type does not reach it.
                    @SuppressWarnings("unchecked")
                    var keepAsProjection = (SelectJoinStep<Record1<String>>) (SelectJoinStep<?>) keep;
                    whereClause.accept(tbSlice, keepAsProjection);

                    var keepQuery = keep.getQuery();
                    var sliceSort = order != null ? order.apply(tbSlice) : null;
                    if (sliceSort != null && !sliceSort.isEmpty()) {
                        keepQuery.addOrderBy(sliceSort);
                    }
                    if (limit != null) {
                        keepQuery.addLimit(limit.intValue());
                    }
                    if (offset != null) {
                        keepQuery.addOffset(offset.intValue());
                    }

                    aggQuery.addConditions(primaryKeyOf(tbRoot).in(keepQuery));
                }

                return aggQuery;
            }

            @Override
            public boolean projectsBeyond(B prjBean, Collection<String> fields) {
                me.runLazyInit();
                if (prjBean == null) {
                    return false;
                }
                for (var e : me.fieldPresenceMap.entrySet()) {
                    if (!fields.contains(e.getKey()) && e.getValue().apply(prjBean) != null) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public B parseJson(String json) {
                me.runLazyInit();
                B bean = me.beanFactory.get();
                try (var strReader = new StringReader(json);
                        var jsonReader = new JsonReader(strReader)) {
                    jsonReader.setStrictness(Strictness.LENIENT);
                    me.doParseJson(bean, jsonReader);
                } catch (IOException e) {
                    throw new IllegalStateException("JSON parse error", e);
                }
                return bean;
            }

            @Override
            public B parseJson(JsonReader jsonReader) {
                me.runLazyInit();
                B bean = me.beanFactory.get();
                try {
                    me.doParseJson(bean, jsonReader);
                } catch (IOException e) {
                    throw new IllegalStateException("JSON parse error", e);
                }
                return bean;
            }

            @Override
            public B fetchOne(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause) {
                var query = this.select(prjBean, whereClause);
                var rec = query.fetchOne();
                if (rec != null && rec.value1() != null) {
                    return this.parseJson(rec.value1());
                }
                return null;
            }

            @Override
            public Stream<B> fetch(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause) {
                var query = this.select(prjBean, whereClause);
                return query.stream()
                        .filter(rec -> rec.value1() != null)
                        .map(rec -> this.parseJson(rec.value1()));
            }

            @Override
            public List<B> fetchToList(B prjBean, BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause) {
                try (var stream = this.fetch(prjBean, whereClause)) {
                    return stream.toList();
                }
            }

            @Override
            public int fetchCount(BiConsumer<T, SelectJoinStep<Record1<Integer>>> whereClause) {
                var ctx = new QueryContext(me.requireDslContext());
                var root = me.tableFactory.apply(ctx.alias(me.tableName));
                var q = ctx.dsl()
                        .selectCount()
                        .from(root);
                whereClause.accept(root, q);
                return q.fetchOne().value1();
            }
        };
    }

    // :: Internal

    private void doParseJson(B bean, JsonReader reader) throws IOException {
        reader.beginObject();
        while (reader.hasNext()) {
            var attrName = reader.nextName();
            var setter = this.fieldSetterMap.get(attrName);
            if (setter != null) {
                setter.accept(bean, reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
    }

}
