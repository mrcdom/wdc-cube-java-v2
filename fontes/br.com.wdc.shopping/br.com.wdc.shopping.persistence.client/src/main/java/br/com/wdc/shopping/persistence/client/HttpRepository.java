package br.com.wdc.shopping.persistence.client;

import java.util.List;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.model.Page;
import br.com.wdc.shopping.domain.repositories.Repository;

/**
 * Implementação genérica de repositório HTTP que delega serialização ao {@link ModelCodec}.
 * <p>
 * Elimina a duplicação entre clientes REST (Gson/JVM) e TeaVM (parsing manual),
 * mantendo uma única lógica de comunicação com a API.
 *
 * @param <E> tipo da entidade
 * @param <C> tipo do critério de pesquisa
 * @param <K> tipo da chave primária
 */
public abstract class HttpRepository<E, C, K> implements Repository<E, C, K> {

    private final HttpTransport transport;
    private final ModelCodec<E, C> codec;
    private final String basePath;

    protected HttpRepository(HttpTransport transport, ModelCodec<E, C> codec, String basePath) {
        this.transport = transport;
        this.codec = codec;
        this.basePath = basePath;
    }

    protected HttpTransport transport() {
        return transport;
    }

    protected String basePath() {
        return basePath;
    }

    @Override
    public boolean insert(E entity) {
        var body = codec.entityToJson(entity);
        var result = transport.postJson(basePath + "/insert", body);
        boolean success = result.get("success").getAsBoolean();
        if (success && result.has("id") && !result.get("id").isJsonNull()) {
            codec.setGeneratedId(entity, result.get("id").getAsLong());
        }
        return success;
    }

    @Override
    public boolean update(E newEntity, E oldEntity) {
        var body = new JsonObject();
        body.add("newEntity", codec.entityToJson(newEntity));
        body.add("oldEntity", codec.entityToJson(oldEntity));
        return transport.postJson(basePath + "/update", body).get("success").getAsBoolean();
    }

    @Override
    public int delete(C criteria) {
        return transport.postJson(basePath + "/delete", codec.criteriaToJson(criteria)).get("count").getAsInt();
    }

    @Override
    public int count(C criteria) {
        return transport.postJson(basePath + "/count", codec.criteriaToJson(criteria)).get("count").getAsInt();
    }

    @Override
    public List<E> fetch(C criteria, int offset, int limit) {
        var body = codec.criteriaToJson(criteria);
        codec.addProjection(body, codec.getProjection(criteria));
        if (offset > 0) body.addProperty("offset", offset);
        if (limit > 0) body.addProperty("limit", limit);
        var result = transport.postJson(basePath + "/fetch", body);
        return codec.entityListFromJson(result.getAsJsonArray("items"));
    }

    @Override
    public Page<E> fetchPage(C criteria, int page, int pageSize) {
        var body = codec.criteriaToJson(criteria);
        codec.addProjection(body, codec.getProjection(criteria));
        if (page > 0) body.addProperty("page", page);
        if (pageSize > 0) body.addProperty("pageSize", pageSize);
        var result = transport.postJson(basePath + "/fetch-page", body);
        var items = codec.entityListFromJson(result.getAsJsonArray("items"));
        int totalItems = result.get("totalItems").getAsInt();
        return Page.of(items, page, pageSize, totalItems);
    }

    @Override
    public E fetchById(K id, E projection) {
        var body = new JsonObject();
        body.addProperty("id", ((Number) id).longValue());
        codec.addProjection(body, projection);
        var result = transport.postJsonNullable(basePath + "/fetch-by-id", body);
        if (result == null) return null;
        return codec.entityFromJson(result);
    }
}
