package br.com.wdc.framework.domain.criteria;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectInput;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.SerializationToken;
import br.com.wdc.framework.domain.exception.InvalidRequestException;

/**
 * How a criteria field travels.
 *
 * <p>
 * Written once, here, instead of repeated in each {@code XxxModelCodec}: what changes from field to field is the value
 * type, not the shape. Each entity codec only says which value reader/writer to use.
 * </p>
 *
 * <p>
 * The shape is an object with the requests:
 * </p>
 *
 * <pre>
 * "price": { "or": true, "p": [ { "o": "GE", "v": [10.0] }, { "o": "IS_NULL" } ] }
 * </pre>
 *
 * <p>
 * <b>A bare value would not be enough.</b> A field carries several requests, each with its operator and its arity —
 * {@code BETWEEN} takes two values, {@code IN} takes many, {@code IS NULL} none — and the disjunction belongs to the
 * field, not to the request. Reducing that to {@code "price": 10.0} would discard everything but equality, and
 * silently: the other side would receive a looser filter than requested, returning extra rows with nothing pointing out
 * the loss.
 * </p>
 */
public final class CriterionCodec {

    /** How to write a value of the field's type. */
    @FunctionalInterface
    public interface ValueWriter<T> {
        void write(ExtensibleObjectOutput out, T value);
    }

    /** How to read a value of the field's type. */
    @FunctionalInterface
    public interface ValueReader<T> {
        T read(ExtensibleObjectInput in);
    }


    public static final ValueWriter<Long> LONG_OUT = (out, v) -> out.value(v.longValue());
    public static final ValueReader<Long> LONG_IN = InputCoerceUtils::asLong;

    public static final ValueWriter<Integer> INT_OUT = (out, v) -> out.value(v.longValue());
    public static final ValueReader<Integer> INT_IN = InputCoerceUtils::asInteger;

    public static final ValueWriter<Double> DOUBLE_OUT = (out, v) -> out.value(v.doubleValue());
    public static final ValueReader<Double> DOUBLE_IN = InputCoerceUtils::asDouble;

    public static final ValueWriter<String> STRING_OUT = (out, v) -> out.value(v);
    public static final ValueReader<String> STRING_IN = InputCoerceUtils::asString;

    public static final ValueWriter<Boolean> BOOL_OUT = (out, v) -> out.value(v.booleanValue());
    public static final ValueReader<Boolean> BOOL_IN = InputCoerceUtils::asBoolean;

    /** Date and time as ISO-8601 text — the form in which the rest of the transport writes them. */
    public static final ValueWriter<OffsetDateTime> ODT_OUT =
            (out, v) -> out.value(v.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
    public static final ValueReader<OffsetDateTime> ODT_IN = (in) -> {
        var text = InputCoerceUtils.asString(in);
        return text == null ? null : OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    };

    private CriterionCodec() {
        // NOOP
    }

    /**
     * Reads the ordering name, rejecting what it does not recognize.
     *
     * <p>
     * <b>Unlike an unknown operator, which is discarded</b> (see {@link #parseOperator}), here the read fails. The
     * difference is what each discard would produce: without a request, the filter gets looser and the field is still
     * listed — the mismatch shows; without the ordering, the list comes back in arbitrary order, and whoever displays it
     * presents it as if it were the requested order. The other side has no way to notice.
     * </p>
     *
     * <p>
     * Takes the accepted values instead of the {@code Class} because {@code Enum.valueOf(Class, String)} depends on
     * reflection, and this code is also compiled for the browser by TeaVM.
     * </p>
     *
     * @throws InvalidRequestException if the name is not among the accepted ones — which the REST layer returns as 400,
     *         naming the received value and the accepted ones.
     */
    public static <E extends Enum<E>> E readOrderBy(ExtensibleObjectInput in, E[] accepted) {
        var name = InputCoerceUtils.asString(in);
        if (name == null) {
            return null;
        }
        for (var candidate : accepted) {
            if (candidate.name().equals(name)) {
                return candidate;
            }
        }
        var acceptedNames = new StringBuilder();
        for (var candidate : accepted) {
            if (acceptedNames.length() > 0) {
                acceptedNames.append(", ");
            }
            acceptedNames.append(candidate.name());
        }
        throw new InvalidRequestException(
                "unknown ordering: '" + name + "' — accepted: " + acceptedNames);
    }

    /**
     * Writes the field, if there is something to write.
     *
     * <p>
     * A field that is not set produces no key at all — this keeps short the criteria that filters by a single field, the
     * common case.
     * </p>
     */
    public static <T> void write(ExtensibleObjectOutput out, String name, Criterion<?, T> criterion,
            ValueWriter<T> valueWriter) {

        if (criterion == null || !criterion.isSet()) {
            return;
        }

        out.name(name).beginObject();
        if (criterion.disjunctive()) {
            out.name("or").value(true);
        }
        out.name("p").beginArray();
        for (var predicate : criterion.predicates()) {
            out.beginObject();
            out.name("o").value(predicate.operator().name());
            if (!predicate.values().isEmpty()) {
                out.name("v").beginArray();
                for (T value : predicate.values()) {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        valueWriter.write(out, value);
                    }
                }
                out.endArray();
            }
            out.endObject();
        }
        out.endArray();
        out.endObject();
    }

    /** The name the alternative groups would have on the wire, where they are refused. */
    public static final String ANY_OF = "anyOf";

    /**
     * Refuses to write a criteria that has alternative groups ({@link Criteria#anyOf()}): they are for code on the
     * server, and do not travel.
     *
     * <p>
     * A disjunction across fields chosen by the client would let it send any tree of {@code OR}s over any columns —
     * the queries the provisioned orderings exist to avoid. Leaving the groups out instead would send a looser filter
     * than the one built, with nothing pointing out the loss; hence the failure, on the side that built it. A search
     * of one term over several columns is a criteria field of its own, which the repository translates with
     * {@code CriterionTranslator.translateAny}.
     * </p>
     *
     * @throws InvalidRequestException if the criteria has groups
     */
    public static void refuseAnyOf(Criteria criteria) {
        if (criteria != null && !criteria.anyOf().isEmpty()) {
            throw anyOfDoesNotTravel();
        }
    }

    /**
     * The failure for alternative groups on the wire — thrown by the reader that meets an {@value #ANY_OF} key, which
     * the REST layer returns as 400 instead of skipping it and answering with a looser filter.
     */
    public static InvalidRequestException anyOfDoesNotTravel() {
        return new InvalidRequestException(
                "alternative groups (anyOf) are server-side only and do not travel; search several columns with a "
                        + "criteria field of its own");
    }

    /**
     * Reads the field in the format above, restoring the requests into {@code criterion}.
     *
     * <p>
     * Restores through {@link Criterion#restore}, not through the fluent methods: {@code eq(null)} adds no request —
     * right for whoever builds a screen filter — and would silently erase what the other side sent.
     * </p>
     *
     * <p>
     * <b>It also accepts a bare value</b> ({@code "price": 10.0}), reading it as equality — the minimal form of an
     * equality filter, and understanding it costs one {@code if}.
     * </p>
     */
    public static <T> void read(ExtensibleObjectInput in, Criterion<?, T> criterion, ValueReader<T> valueReader) {
        if (in.peek() != SerializationToken.BEGIN_OBJECT) {
            var value = valueReader.read(in);
            if (value != null) {
                criterion.restore(Operator.EQ, List.of(value));
            }
            return;
        }

        in.beginObject();
        while (in.hasNext()) {
            switch (in.nextName()) {
            case "or" -> criterion.restoreDisjunctive(Boolean.TRUE.equals(InputCoerceUtils.asBoolean(in)));
            case "p" -> readPredicates(in, criterion, valueReader);
            default -> in.skipValue();
            }
        }
        in.endObject();
    }

    private static <T> void readPredicates(ExtensibleObjectInput in, Criterion<?, T> criterion,
            ValueReader<T> valueReader) {

        in.beginArray();
        while (in.hasNext()) {
            Operator operator = null;
            var values = new ArrayList<T>(2);

            in.beginObject();
            while (in.hasNext()) {
                switch (in.nextName()) {
                case "o" -> operator = parseOperator(InputCoerceUtils.asString(in));
                case "v" -> {
                    in.beginArray();
                    while (in.hasNext()) {
                        values.add(in.peek() == SerializationToken.NULL ? in.nextNull() : valueReader.read(in));
                    }
                    in.endArray();
                }
                default -> in.skipValue();
                }
            }
            in.endObject();

            if (operator != null) {
                criterion.restore(operator, values);
            }
        }
        in.endArray();
    }

    /**
     * An unknown operator is discarded, not translated by approximation.
     *
     * <p>
     * It happens when one side is newer than the other and sends an operator this one does not know yet. Picking the
     * "closest" one would produce a filter different from the one requested, with nothing signaling it; leaving the
     * request out produces a looser filter, which is equally wrong — but the field is still listed, and the mismatch
     * shows.
     * </p>
     */
    private static Operator parseOperator(String name) {
        if (name == null) {
            return null;
        }
        try {
            return Operator.valueOf(name);
        } catch (IllegalArgumentException _ignore) {
            return null;
        }
    }
}
