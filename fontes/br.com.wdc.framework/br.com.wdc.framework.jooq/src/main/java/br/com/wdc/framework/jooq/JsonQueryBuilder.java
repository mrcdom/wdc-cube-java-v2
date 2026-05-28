package br.com.wdc.framework.jooq;

import java.io.IOException;
import java.io.StringReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.SelectJoinStep;
import org.jooq.SelectQuery;
import org.jooq.Table;
import org.jooq.impl.DSL;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.function.ThrowingConsumer;

/**
 * Query builder declarativo para mapeamento bean ↔ tabela JOOQ com projeção JSON.
 *
 * <p>
 * Suporte a <b>projeção</b>: bean com campos não-nulos indica quais colunas incluir no SELECT.
 * Suporte a <b>lazy</b>: registro de coleções filhas é diferido até primeira execução.
 * Coleções (1:N) via subselect correlacionado com JSON array.
 * Apenas para SELECTs — sem suporte a insert/update.
 * </p>
 *
 * <p>
 * Uso típico (no repositório):
 *
 * <pre>{@code
 * private static final JsonQuery<Usuario, TUsuario> QUERY =
 *     new JsonQueryBuilder<Usuario, TUsuario>()
 *         .setAlias("u")
 *         .setBeanFactory(Usuario::new)
 *         .setTableFactory(alias -> USUARIO.as(alias))
 *         .addI64("id", u -> u.id, (u, v) -> u.id = v, t -> t.ID)
 *         .addStr("login", u -> u.login, (u, v) -> u.login = v, t -> t.LOGIN)
 *         .build();
 *
 * // Projeção total:
 * Usuario prj = QUERY.newProjectionBean();
 * List<Usuario> list = QUERY.fetchToList(prj, (t, q) -> q.where(t.STATUS.eq("ATIVO")));
 *
 * // Projeção parcial (somente id e login):
 * Usuario prj = QUERY.newProjectionBean(u -> { u.nome = null; u.perfil = null; ... });
 * }</pre>
 *
 * @param <B> tipo do bean de domínio
 * @param <T> tipo da classe-schema (ex: TUsuario)
 */
@SuppressWarnings("java:S4276") // getters usam tipo boxed como sentinela de projeção (null = campo excluído do SELECT)
public class JsonQueryBuilder<B, T extends Table<?>> {

    // :: Callback funcional para projeção (chain of responsibility)

    @FunctionalInterface
    private interface FieldProjectionCallback<B, T> {

        @SuppressWarnings("unchecked")
        static <B, T> FieldProjectionCallback<B, T> noop() {
            return (FieldProjectionCallback<B, T>) NOOP;
        }

        FieldProjectionCallback<?, ?> NOOP = (f, c, b, t) -> {
        };

        void accept(List<Pair<String, Field<String>>> fields, QueryContext ctx, B bean, T table);

        default FieldProjectionCallback<B, T> andThen(FieldProjectionCallback<? super B, ? super T> after) {
            Objects.requireNonNull(after);
            return (fields, ctx, bean, table) -> {
                accept(fields, ctx, bean, table);
                after.accept(fields, ctx, bean, table);
            };
        }
    }

    // :: Callback funcional para parsing JSON

    @FunctionalInterface
    private interface FieldSetterCallback<B> {
        void accept(B bean, JsonReader reader) throws IOException;
    }

    // :: Campos do builder

    private String tableName;
    private Supplier<B> beanFactory;
    private Function<String, T> tableFactory;
    private Consumer<B> fillProjBean;
    private FieldProjectionCallback<B, T> fieldPrjList;
    private Map<String, FieldSetterCallback<B>> fieldSetterMap;
    private Consumer<JsonQueryBuilder<B, T>> lazyInit; // guardado por lazyLock durante a inicialização
    private final ReentrantLock lazyLock = new ReentrantLock();
    private volatile boolean lazyDone = false;

    public JsonQueryBuilder() {
        this.fieldSetterMap = new HashMap<>();
        this.lazyInit = ThrowingConsumer.noop();
        this.fieldPrjList = FieldProjectionCallback.noop();
        this.fillProjBean = ThrowingConsumer.noop();
    }

    // :: Configuração

    /** Alias base para a tabela principal (ex: "u" para usuario). */
    public JsonQueryBuilder<B, T> setAlias(String name) {
        this.tableName = name;
        return this;
    }

    /** Factory do bean de domínio (ex: {@code Usuario::new}). */
    public JsonQueryBuilder<B, T> setBeanFactory(Supplier<B> beanFactory) {
        this.beanFactory = beanFactory;
        return this;
    }

    /** Factory da tabela-schema com alias (ex: {@code alias -> USUARIO.as(alias)}). */
    public JsonQueryBuilder<B, T> setTableFactory(Function<String, T> tableFactory) {
        this.tableFactory = tableFactory;
        return this;
    }



    // :: Registro de campos escalares

    /** Campo Long (BIGINT). */
    public JsonQueryBuilder<B, T> addI64(String fn, Function<B, Long> getter,
            BiConsumer<B, Long> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0L));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonNum(fn, jooqField.apply(table)));
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

    /** Campo Integer (INT). */
    public JsonQueryBuilder<B, T> addI32(String fn, Function<B, Integer> getter,
            BiConsumer<B, Integer> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonNum(fn, jooqField.apply(table)));
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
     * Campo Enum escalar (VARCHAR no banco, enum Java na aplicação).
     *
     * <p>
     * O {@code parser} deve ser o método de lookup do próprio enum (ex: {@code Role::parse}). Deve retornar
     * {@code null} para valores desconhecidos ou nulos — nunca lançar exceção.
     *
     * @param enumClass classe do enum; usada para obter o sentinel de projeção via {@code getEnumConstants()[0]}
     * @param parser    função de deserialização String → E (ex: {@code Role::parse})
     */
    public <E extends Enum<E>> JsonQueryBuilder<B, T> addEnm(String fn,
            Function<B, E> getter,
            BiConsumer<B, E> setter,
            Class<E> enumClass,
            Function<String, E> parser,
            Function<T, Field<String>> jooqField) {

        E sentinel = enumClass.getEnumConstants()[0];

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, sentinel));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonStr(fn, jooqField.apply(table)));
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

    /** Campo String (VARCHAR). */
    public JsonQueryBuilder<B, T> addStr(String fn, Function<B, String> getter,
            BiConsumer<B, String> setter, Function<T, Field<String>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, ""));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonStr(fn, jooqField.apply(table)));
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

    /** Campo Boolean. */
    public JsonQueryBuilder<B, T> addBit(String fn, Function<B, Boolean> getter,
            BiConsumer<B, Boolean> setter, Function<T, Field<Boolean>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, Boolean.FALSE));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonBool(fn, jooqField.apply(table)));
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

    /** Campo OffsetDateTime (TIMESTAMPTZ). */
    public JsonQueryBuilder<B, T> addOdt(String fn, Function<B, OffsetDateTime> getter,
            BiConsumer<B, OffsetDateTime> setter, Function<T, Field<OffsetDateTime>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, OffsetDateTime.MIN));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonOdt(fn, jooqField.apply(table)));
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
     * Campo LocalDateTime (TIMESTAMP sem timezone) mapeado para OffsetDateTime no domínio.
     *
     * <p>
     * Útil quando o banco armazena TIMESTAMP (sem TZ) mas o domínio usa {@link OffsetDateTime}. A leitura aplica parse
     * leniente que aceita tanto ISO-8601 completo quanto o formato H2 ({@code yyyy-MM-dd HH:mm:ss[.n]}).
     * </p>
     */
    public JsonQueryBuilder<B, T> addLdt(String fn, Function<B, OffsetDateTime> getter,
            BiConsumer<B, OffsetDateTime> setter, Function<T, Field<java.time.LocalDateTime>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, OffsetDateTime.MIN));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonOdt(fn, jooqField.apply(table)));
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

    private static OffsetDateTime parseLdt(String s) {
        try {
            return OffsetDateTime.parse(s);
        } catch (java.time.format.DateTimeParseException ex) {
            // H2 format: "2024-01-15 10:30:00" or "2024-01-15 10:30:00.123456"
            var ldt = java.time.LocalDateTime.parse(s.replace(' ', 'T'));
            return ldt.atOffset(java.time.ZoneOffset.UTC);
        }
    }

    /** Campo Double (DOUBLE PRECISION / NUMERIC). */
    public JsonQueryBuilder<B, T> addF64(String fn, Function<B, Double> getter,
            BiConsumer<B, Double> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, 0.0));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonNum(fn, jooqField.apply(table)));
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

    /** Campo BigDecimal (DECIMAL / NUMERIC). */
    public JsonQueryBuilder<B, T> addDec(String fn, Function<B, java.math.BigDecimal> getter,
            BiConsumer<B, java.math.BigDecimal> setter, Function<T, Field<? extends Number>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, java.math.BigDecimal.ZERO));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonNum(fn, jooqField.apply(table)));
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

    /** Campo byte[] (BINARY / BLOB). Serializado como Base64 no JSON. */
    public JsonQueryBuilder<B, T> addBin(String fn, Function<B, byte[]> getter,
            BiConsumer<B, byte[]> setter, Function<T, Field<byte[]>> jooqField) {

        this.fillProjBean = this.fillProjBean.andThen(bean -> setter.accept(bean, new byte[0]));

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx0, bean, table) -> {
            if (getter.apply(bean) != null) {
                fields.add(JooqUtils.toJsonBin(fn, jooqField.apply(table)));
            }
        });

        this.fieldSetterMap.put(fn, (bean, reader) -> {
            if (reader.peek() == JsonToken.STRING) {
                setter.accept(bean, java.util.Base64.getDecoder().decode(reader.nextString()));
            } else {
                reader.skipValue();
            }
        });
        return this;
    }

    // :: Lazy (defer child query registration)

    /**
     * Adia o registro de campos (tipicamente coleções filhas) até a primeira execução. Evita dependências circulares e
     * inicialização cara.
     */
    public JsonQueryBuilder<B, T> lazy(Consumer<JsonQueryBuilder<B, T>> initCallback) {
        var prev = this.lazyInit;
        this.lazyInit = me -> {
            prev.accept(me);
            initCallback.accept(me);
        };
        return this;
    }

    /** Executa a inicialização lazy exatamente uma vez, thread-safe via double-checked locking com ReentrantLock. */
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

    // :: Coleções filhas (1:N) — subselect correlacionado

    /**
     * Registra um campo Set&lt;ChildBean&gt; carregado via subselect correlacionado.
     *
     * @param fn         nome JSON do campo
     * @param getter     getter do Set no bean pai
     * @param setter     setter do Set no bean pai
     * @param childQuery JsonQuery construído para o bean filho
     * @param childWhere callback que define a correlação (WHERE child.fk = parent.pk)
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanSetField(String fn,
            Function<B, Set<C>> getter,
            BiConsumer<B, Set<C>> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var childPrjBeanSet = getter.apply(bean);
            if (childPrjBeanSet == null || childPrjBeanSet.isEmpty()) {
                return;
            }

            var childPrjBean = childPrjBeanSet.iterator().next();
            if (childPrjBean == null) {
                return;
            }

            var childQueryBuilder = new JsonChildQueryBuilder<T, U>(ctx, table);
            if (childPrjBeanSet instanceof HasCriteria hasCriteria) {
                childQueryBuilder.criteria = hasCriteria.getCriteria();
            }
            var clause = (BiConsumer<U, SelectJoinStep<Record1<String>>>) (tbChild, q) -> {
                childQueryBuilder.childTable = tbChild;
                childQueryBuilder.dsl = q;
                childWhereClause.accept(childQueryBuilder);
            };

            fields.add(Pair.of(fn,
                    DSL.field(childQuery.select(ctx, childPrjBean, clause, true))));
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
     * Registra um campo List&lt;ChildBean&gt; carregado via subselect correlacionado.
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanListField(String fn,
            Function<B, List<C>> getter,
            BiConsumer<B, List<C>> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var childPrjBeanList = getter.apply(bean);
            if (childPrjBeanList == null || childPrjBeanList.isEmpty()) {
                return;
            }

            var childPrjBean = childPrjBeanList.getFirst();
            if (childPrjBean == null) {
                return;
            }

            var childQueryBuilder = new JsonChildQueryBuilder<T, U>(ctx, table);
            if (childPrjBeanList instanceof HasCriteria hasCriteria) {
                childQueryBuilder.criteria = hasCriteria.getCriteria();
            }
            var clause = (BiConsumer<U, SelectJoinStep<Record1<String>>>) (tbChild, q) -> {
                childQueryBuilder.childTable = tbChild;
                childQueryBuilder.dsl = q;
                childWhereClause.accept(childQueryBuilder);
            };

            fields.add(Pair.of(fn,
                    DSL.field(childQuery.select(ctx, childPrjBean, clause, true))));
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
     * Registra um campo ChildBean (relação 1:1) carregado via subselect.
     */
    public <C, U extends Table<?>> JsonQueryBuilder<B, T> addBeanField(String fn,
            Function<B, C> getter,
            BiConsumer<B, C> setter,
            JsonQuery<C, U> childQuery,
            Consumer<JsonChildQueryBuilder<T, U>> childWhereClause) {

        this.fieldPrjList = this.fieldPrjList.andThen((fields, ctx, bean, table) -> {
            var childPrjBean = getter.apply(bean);
            if (childPrjBean == null) {
                return;
            }

            var childQueryBuilder = new JsonChildQueryBuilder<T, U>(ctx, table);
            var clause = (BiConsumer<U, SelectJoinStep<Record1<String>>>) (tbChild, q) -> {
                childQueryBuilder.childTable = tbChild;
                childQueryBuilder.dsl = q;
                childWhereClause.accept(childQueryBuilder);
            };

            fields.add(Pair.of(fn,
                    DSL.field(childQuery.select(ctx, childPrjBean, clause, false))));
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
     * Constrói a instância imutável de query pronta para execução.
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
                var ctx = new QueryContext();
                return select(ctx, prjBean, whereClause, false);
            }

            @Override
            public SelectQuery<Record1<String>> select(QueryContext ctx, B prjBean,
                    BiConsumer<T, SelectJoinStep<Record1<String>>> whereClause, boolean isAgg) {
                me.runLazyInit();

                var tbRoot = me.tableFactory.apply(me.tableName + ctx.nextUniqueInt());

                var prjField = isAgg
                        ? DSL.field(DSL.sql(
                                "'['"
                                        + " || coalesce(string_agg(" + projection(ctx, tbRoot, prjBean) + ", ','), '')"
                                        + " || ']'"),
                                String.class)
                        : projection(ctx, tbRoot, prjBean);

                var joinStep = ctx.dsl()
                        .select(prjField)
                        .from(tbRoot);

                whereClause.accept(tbRoot, joinStep);
                return joinStep.getQuery();
            }

            @Override
            public Field<String> projection(QueryContext ctx, T jooqTable, B prjBean) {
                me.runLazyInit();
                var jsonFields = new ArrayList<Pair<String, Field<String>>>();
                me.fieldPrjList.accept(jsonFields, ctx,
                        prjBean != null ? prjBean : this.newProjectionBean(), jooqTable);
                return JooqUtils.toJsonObjectField(jsonFields);
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
                var ctx = new QueryContext();
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
