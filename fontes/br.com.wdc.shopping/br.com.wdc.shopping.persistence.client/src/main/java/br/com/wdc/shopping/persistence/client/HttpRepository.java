package br.com.wdc.shopping.persistence.client;

import java.util.List;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.codec.ModelCodec;
import br.com.wdc.shopping.domain.pagination.Page;
import br.com.wdc.shopping.domain.repositories.Repository;

/**
 * Implementação genérica de repositório HTTP que delega serialização ao {@link ModelCodec}.
 * <p>
 * Usa {@link JsonStreamWriter}/{@link JsonStreamReader} para serialização streaming
 * sem dependência de Gson, compatível com JVM e TeaVM.
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
        var writer = new JsonStreamWriter();
        codec.writeEntity(writer, entity);
        var responseJson = transport.postJson(basePath + "/insert", writer.result());

        var reader = new JsonStreamReader(responseJson);
        reader.beginObject();
        boolean success = false;
        long id = -1;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "success" -> success = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
                case "id" -> {
                    var v = InputCoerceUtils.asLong(reader);
                    if (v != null) id = v;
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();

        if (success && id >= 0) {
            codec.setGeneratedId(entity, id);
        }
        return success;
    }

    @Override
    public boolean update(E newEntity, E oldEntity) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("newEntity");
        codec.writeEntity(writer, newEntity);
        writer.name("oldEntity");
        codec.writeEntity(writer, oldEntity);
        writer.endObject();

        var responseJson = transport.postJson(basePath + "/update", writer.result());
        return readSuccess(responseJson);
    }

    @Override
    public int delete(C criteria) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        codec.writeCriteriaFields(writer, criteria);
        writer.endObject();

        var responseJson = transport.postJson(basePath + "/delete", writer.result());
        return readCount(responseJson);
    }

    @Override
    public int count(C criteria) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        codec.writeCriteriaFields(writer, criteria);
        writer.endObject();

        var responseJson = transport.postJson(basePath + "/count", writer.result());
        return readCount(responseJson);
    }

    @Override
    public List<E> fetch(C criteria, int offset, int limit) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        codec.writeCriteriaFields(writer, criteria);
        writeProjection(writer, codec.getProjection(criteria));
        if (offset > 0) writer.name("offset").value(offset);
        if (limit > 0) writer.name("limit").value(limit);
        writer.endObject();

        var responseJson = transport.postJson(basePath + "/fetch", writer.result());

        var reader = new JsonStreamReader(responseJson);
        reader.beginObject();
        List<E> items = List.of();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "items" -> items = codec.readEntityList(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return items;
    }

    @Override
    public Page<E> fetchPage(C criteria, int page, int pageSize) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        codec.writeCriteriaFields(writer, criteria);
        writeProjection(writer, codec.getProjection(criteria));
        if (page > 0) writer.name("page").value(page);
        if (pageSize > 0) writer.name("pageSize").value(pageSize);
        writer.endObject();

        var responseJson = transport.postJson(basePath + "/fetch-page", writer.result());

        var reader = new JsonStreamReader(responseJson);
        reader.beginObject();
        List<E> items = List.of();
        int totalItems = 0;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "items" -> items = codec.readEntityList(reader);
                case "totalItems" -> totalItems = InputCoerceUtils.asInteger(reader, 0);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return Page.of(items, page, pageSize, totalItems);
    }

    @Override
    public E fetchById(K id, E projection) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("id").value(((Number) id).longValue());
        writeProjection(writer, projection);
        writer.endObject();

        var responseJson = transport.postJsonNullable(basePath + "/fetch-by-id", writer.result());
        if (responseJson == null) return null;

        var reader = new JsonStreamReader(responseJson);
        return codec.readEntity(reader);
    }

    // ── Helpers ──

    private void writeProjection(JsonStreamWriter writer, E projection) {
        if (projection != null) {
            writer.name("projection");
            codec.writeEntity(writer, projection);
        }
    }

    private boolean readSuccess(String responseJson) {
        var reader = new JsonStreamReader(responseJson);
        reader.beginObject();
        boolean success = false;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "success" -> success = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return success;
    }

    private int readCount(String responseJson) {
        var reader = new JsonStreamReader(responseJson);
        reader.beginObject();
        int count = 0;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "count" -> count = InputCoerceUtils.asInteger(reader, 0);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return count;
    }
}
