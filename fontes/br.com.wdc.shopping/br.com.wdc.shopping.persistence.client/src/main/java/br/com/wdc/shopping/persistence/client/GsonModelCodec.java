package br.com.wdc.shopping.persistence.client;

import java.lang.reflect.Type;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Implementação de {@link ModelCodec} baseada em Gson (reflection).
 * <p>
 * Adequada para ambientes JVM completos (Swing, Gluon, testes).
 * Não compatível com TeaVM.
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 */
public class GsonModelCodec<E, C> implements ModelCodec<E, C> {

    private final Gson gson;
    private final Class<E> entityClass;
    private final Type listType;
    private final Function<C, JsonObject> criteriaSerializer;
    private final Function<C, E> projectionGetter;
    private final BiConsumer<E, Long> idSetter;
    private final Function<E, JsonObject> entitySerializer;

    public GsonModelCodec(
            Gson gson,
            Class<E> entityClass,
            Type listType,
            Function<C, JsonObject> criteriaSerializer,
            Function<C, E> projectionGetter,
            BiConsumer<E, Long> idSetter) {
        this(gson, entityClass, listType, criteriaSerializer, projectionGetter, idSetter, null);
    }

    public GsonModelCodec(
            Gson gson,
            Class<E> entityClass,
            Type listType,
            Function<C, JsonObject> criteriaSerializer,
            Function<C, E> projectionGetter,
            BiConsumer<E, Long> idSetter,
            Function<E, JsonObject> entitySerializer) {
        this.gson = gson;
        this.entityClass = entityClass;
        this.listType = listType;
        this.criteriaSerializer = criteriaSerializer;
        this.projectionGetter = projectionGetter;
        this.idSetter = idSetter;
        this.entitySerializer = entitySerializer;
    }

    @Override
    public JsonObject entityToJson(E entity) {
        if (entitySerializer != null) return entitySerializer.apply(entity);
        return gson.toJsonTree(entity).getAsJsonObject();
    }

    @Override
    public E entityFromJson(JsonObject json) {
        return gson.fromJson(json, entityClass);
    }

    @Override
    public List<E> entityListFromJson(JsonArray array) {
        return gson.fromJson(array, listType);
    }

    @Override
    public JsonObject criteriaToJson(C criteria) {
        return criteriaSerializer.apply(criteria);
    }

    @Override
    public E getProjection(C criteria) {
        return projectionGetter.apply(criteria);
    }

    @Override
    public void addProjection(JsonObject body, E projection) {
        if (projection != null) {
            body.add("projection", gson.toJsonTree(projection));
        }
    }

    @Override
    public void setGeneratedId(E entity, long id) {
        idSetter.accept(entity, id);
    }
}
