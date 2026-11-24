package br.com.wdc.framework.domain.codec;

import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.EntityGraph;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;

/**
 * Serialization/deserialization contract for a repository's entities and criteria.
 * <p>
 * Uses {@link ExtensibleObjectOutput}/{@link ExtensibleObjectInput} for streaming
 * serialization, compatible with the JVM and TeaVM with no dependency on Gson/reflection.
 *
 * @param <E> entity type
 * @param <C> search criteria type
 */
public interface ModelCodec<E, C> {

    /**
     * Result of parsing an entity for update: entity + projection (fields present in the JSON).
     */
    record UpdateData<T>(T entity, T projection) {
    }

    /**
     * Writes the entity as a complete JSON object (beginObject + fields + endObject).
     */
    void writeEntity(ExtensibleObjectOutput out, E entity);

    /**
     * Writes the entity with graph tracking — entities already seen are written with the key only.
     * Implementations must use {@link EntityGraph#track(br.com.wdc.framework.commons.serialization.KeyedEntity)} to
     * detect repetitions before serializing nested fields.
     */
    void writeEntity(ExtensibleObjectOutput out, E entity, EntityGraph graph);

    /**
     * Writes only the entity fields marked by the projection (fields that are non-null in the projection).
     * The ID is always included.
     */
    void writeEntityProjected(ExtensibleObjectOutput out, E entity, E projection);

    /**
     * Computes the projection from the difference between two entities.
     * Returns a projection with fields marked (non-null) for each field that differs between newEntity and oldEntity.
     */
    E computeProjection(E newEntity, E oldEntity);

    /**
     * Reads an entity from the current position (expects beginObject).
     */
    E readEntity(ExtensibleObjectInput in);

    /**
     * Reads an entity for update, returning both the entity and the projection
     * (fields present in the JSON get a non-null marker in the projection).
     */
    UpdateData<E> readEntityForUpdate(ExtensibleObjectInput in);

    /**
     * Reads a list of entities (expects beginArray).
     */
    default List<E> readEntityList(ExtensibleObjectInput in) {
        var list = new ArrayList<E>();
        in.beginArray();
        while (in.hasNext()) {
            list.add(readEntity(in));
        }
        in.endArray();
        return list;
    }

    /**
     * Writes the criteria fields into the current object (without begin/endObject).
     */
    void writeCriteriaFields(ExtensibleObjectOutput out, C criteria);

    /**
     * Reads the criteria fields starting at the current field name (without begin/endObject).
     * Returns true if the field was consumed, false if it is not a recognized criteria field.
     */
    boolean readCriteriaField(ExtensibleObjectInput in, String fieldName, C criteria);

    /**
     * Returns the projection associated with the criteria (may be null).
     */
    E getProjection(C criteria);

    /**
     * Sets the server-generated ID on the entity after insert.
     */
    void setGeneratedId(E entity, long id);
}
