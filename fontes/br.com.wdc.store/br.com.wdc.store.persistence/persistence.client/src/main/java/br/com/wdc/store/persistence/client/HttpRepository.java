package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.codec.ModelCodec;
import br.com.wdc.framework.domain.repository.Repository;

/**
 * Generic HTTP implementation of a repository that reads and writes: the reads of {@link HttpReadOnlyRepository},
 * plus {@code insert}, {@code update} and {@code delete}.
 *
 * @param <E> entity type
 * @param <C> search criteria type
 * @param <K> primary key type
 */
public abstract class HttpRepository<E, C, K> extends HttpReadOnlyRepository<E, C, K> implements Repository<E, C, K> {

    protected HttpRepository(HttpTransport transport, ModelCodec<E, C> codec, String basePath) {
        super(transport, codec, basePath);
    }

    @Override
    public boolean insert(E entity) {
        var writer = new JsonStreamWriter();
        codec().writeEntity(writer, entity);
        var responseJson = transport().postJson(basePath() + "/insert", writer.result());

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
            codec().setGeneratedId(entity, id);
        }
        return success;
    }

    @Override
    public boolean update(E newEntity, E oldEntity, E projection) {
        E effectiveProjection;
        if (projection != null) {
            effectiveProjection = projection;
        } else if (oldEntity != null) {
            effectiveProjection = codec().computeProjection(newEntity, oldEntity);
        } else {
            effectiveProjection = newProjection();
        }

        var writer = new JsonStreamWriter();
        codec().writeEntityProjected(writer, newEntity, effectiveProjection);

        var responseJson = transport().postJson(basePath() + "/update", writer.result());
        return readSuccess(responseJson);
    }

    @Override
    public int delete(C criteria) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        codec().writeCriteriaFields(writer, criteria);
        writer.endObject();

        var responseJson = transport().postJson(basePath() + "/delete", writer.result());
        return readCount(responseJson);
    }

    // ── Helpers ──

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
}
