package br.com.wdc.store.persistence.client;

import java.util.List;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.pagination.Page;
import br.com.wdc.framework.domain.repository.ReadOnlyRepository;

/**
 * Generic HTTP implementation of the reads of a repository ({@code count}, {@code fetch}, {@code fetch-page}), which
 * delegates serialization to the {@link ModelCodec}. {@link HttpRepository} adds the writes.
 * <p>
 * Uses {@link JsonStreamWriter}/{@link JsonStreamReader} for streaming serialization
 * with no Gson dependency, compatible with the JVM and TeaVM.
 *
 * @param <E> entity type
 * @param <C> search criteria type
 * @param <K> primary key type
 */
public abstract class HttpReadOnlyRepository<E, C, K> implements ReadOnlyRepository<E, C, K> {

    private final HttpTransport transport;
    private final ModelCodec<E, C> codec;
    private final String basePath;

    protected HttpReadOnlyRepository(HttpTransport transport, ModelCodec<E, C> codec, String basePath) {
        this.transport = transport;
        this.codec = codec;
        this.basePath = basePath;
    }

    protected HttpTransport transport() {
        return transport;
    }

    protected ModelCodec<E, C> codec() {
        return codec;
    }

    protected String basePath() {
        return basePath;
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


    // ── Helpers ──

    protected void writeProjection(JsonStreamWriter writer, E projection) {
        if (projection != null) {
            writer.name("projection");
            codec.writeEntity(writer, projection);
        }
    }

    protected int readCount(String responseJson) {
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
