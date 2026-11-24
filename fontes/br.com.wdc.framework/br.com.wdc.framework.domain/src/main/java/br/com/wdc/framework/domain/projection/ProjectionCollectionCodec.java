package br.com.wdc.framework.domain.projection;

import java.util.Collection;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.util.HasCriteria;
import br.com.wdc.framework.commons.util.HasSlice;

/**
 * How a <b>projected collection</b> of a 1:N relation travels.
 *
 * <p>
 * A projection collection is not a list of results: it is <b>one</b> item shape plus what to do with the collection —
 * the criteria that filters it ({@link HasCriteria}) and the slice to apply ({@link HasSlice}). These three pieces must
 * reach the other side, or the collection comes back whole and unordered.
 * </p>
 *
 * <p>
 * <b>The wire shape alone tells a projection from a result.</b> The projection is an object; the result is the usual
 * array:
 * </p>
 *
 * <pre>
 * // projection (client → server)
 * "items": { "shape": {…one item…}, "where": {…criteria…}, "limit": 5, "offset": 0 }
 *
 * // result (server → client) — plain array
 * "items": [ {…}, {…}, {…} ]
 * </pre>
 *
 * <p>
 * The reader decides by the token: an object is a projection, an array is a result. So the same {@code items} field
 * serves both directions without a second name, and the result keeps its plain array form.
 * </p>
 *
 * <p>
 * The embedded criteria uses the same expressive structure as the top-level criteria — it is the child entity's codec
 * that writes and reads it, through the callbacks. The two codecs cooperate: this one handles the collection envelope,
 * that one the criteria contents.
 * </p>
 */
public final class ProjectionCollectionCodec {

    /** Writes the shape of a collection item. */
    @FunctionalInterface
    public interface ShapeWriter<E> {
        void write(ExtensibleObjectOutput out, E item);
    }

    /** Reads the shape of a collection item. */
    @FunctionalInterface
    public interface ShapeReader<E> {
        E read(ExtensibleObjectInput in);
    }

    /** Writes the embedded criteria — complete object, {@code begin/endObject} included. */
    @FunctionalInterface
    public interface CriteriaWriter {
        void write(ExtensibleObjectOutput out, Object criteria);
    }

    /** Reads the embedded criteria — complete object, {@code begin/endObject} included. */
    @FunctionalInterface
    public interface CriteriaReader {
        Object read(ExtensibleObjectInput in);
    }

    private ProjectionCollectionCodec() {
        // NOOP
    }

    /**
     * Writes the projected collection as the envelope {@code { shape, where?, limit?, offset? }}.
     *
     * <p>
     * Only the shape is mandatory — it is the first element of the collection. Criteria and slice are included when the
     * collection carries them; when absent, the key does not appear at all.
     * </p>
     */
    public static <E> void write(ExtensibleObjectOutput out, String name, Collection<E> collection,
            ShapeWriter<E> shapeWriter, CriteriaWriter criteriaWriter) {

        var shape = collection.iterator().next();

        out.name(name).beginObject();

        out.name("shape");
        shapeWriter.write(out, shape);

        if (collection instanceof HasCriteria hc && hc.getCriteria() != null && criteriaWriter != null) {
            out.name("where");
            criteriaWriter.write(out, hc.getCriteria());
        }
        if (collection instanceof HasSlice hs) {
            if (hs.getLimit() != null) {
                out.name("limit").value(hs.getLimit().longValue());
            }
            if (hs.getOffset() != null) {
                out.name("offset").value(hs.getOffset().longValue());
            }
        }

        out.endObject();
    }

    /** Whether the field's current value is a projected collection (object) rather than a list of results (array). */
    public static boolean isProjectionEnvelope(ExtensibleObjectInput in) {
        return in.peek() == br.com.wdc.framework.commons.serialization.SerializationToken.BEGIN_OBJECT;
    }

    /**
     * Reads the envelope and returns the ready projection collection — shape, criteria and slice rehydrated.
     */
    public static <E> ProjectionList<E> read(ExtensibleObjectInput in, ShapeReader<E> shapeReader,
            CriteriaReader criteriaReader) {

        E shape = null;
        Object criteria = null;
        Integer limit = null;
        Integer offset = null;

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
            case "shape" -> shape = shapeReader.read(in);
            case "where" -> criteria = criteriaReader.read(in);
            case "limit" -> limit = InputCoerceUtils.asInteger(in);
            case "offset" -> offset = InputCoerceUtils.asInteger(in);
            default -> in.skipValue();
            }
        }
        in.endObject();

        var list = new ProjectionList<E>(shape, criteria);
        if (limit != null) {
            list.withLimit(limit);
        }
        if (offset != null) {
            list.withOffset(offset);
        }
        return list;
    }
}
