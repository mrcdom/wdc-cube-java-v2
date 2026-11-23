package br.com.wdc.framework.cube.remote;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.ActionOf2;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;

/**
 * Command class that serializes a {@link Scope} to JSON.
 *
 * <p>
 * Each instance encapsulates the context of a single serialization execution.
 * </p>
 *
 * <ul>
 * <li>Injects {@code "#"} as the first field (instanceId of the view).</li>
 * <li>{@code transient} and {@code static} fields are skipped, and so are actions ({@code Action}, {@code ActionOf},
 * {@code ActionOf2}).</li>
 * <li>Supported field types: all Java primitives/wrappers, {@link String}, {@link Character},
 * dates, {@link Collection}, {@link Map}, Records, and POJOs with public fields.</li>
 * <li>Blank strings in direct fields are omitted.</li>
 * <li>A {@link Prop} field is written as its value.</li>
 * <li>A field holding a {@link Scope} (a presenter's below, a segment) is written as {@code "<name>ViewId": vsid}, the id
 * of the view that draws it.</li>
 * <li>A field holding a list of segments (rows) is written as {@code "<name>ViewIds": [vsid, ...]}, in order.</li>
 * <li>Numeric primitives and boolean primitives are always serialized.</li>
 * </ul>
 */
public final class ScopeSerializer {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ISO_LOCAL_TIME;

    private final ExtensibleObjectOutput json;

    private final Function<Scope, CubeView> viewOf;

    /**
     * @param viewOf the view of a scope inside the one written (a segment, a row); the remote host makes it on demand
     */
    public ScopeSerializer(ExtensibleObjectOutput json, Function<Scope, CubeView> viewOf) {
        this.json = json;
        this.viewOf = viewOf;
    }

    public void write(Object state, String instanceId) {
        try {
            json.beginObject();
            json.name("#").value(instanceId);

            for (Field field : state.getClass().getFields()) {
                if (Modifier.isTransient(field.getModifiers()) || Modifier.isStatic(field.getModifiers()))
                    continue;

                Object value = field.get(state);
                writeField(field, value);
            }

            json.endObject();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to serialize Scope: " + state.getClass().getSimpleName(), e);
        }
    }

    private void writeField(Field field, Object value) {
        Class<?> type = field.getType();

        // An observed field: what goes out is its value, with the same rules as a plain field of the value's type
        if (value instanceof Prop<?> prop) {
            value = prop.get();
            type = value != null ? value.getClass() : Object.class;
        }

        if (type == boolean.class) {
            json.name(field.getName()).value((boolean) value);
            return;
        }

        if (value == null)
            return;

        // Actions are the presenter's entry points, not state: the client fires them by event code
        if (value instanceof Action || value instanceof ActionOf<?> || value instanceof ActionOf2<?, ?>)
            return;

        // A list of segments (rows): each row is a view of its own, referred to by id, in order
        if (isSegmentList(field)) {
            json.name(field.getName() + "ViewIds");
            json.beginArray();
            for (Object item : (Collection<?>) value) {
                var rowView = item instanceof Scope row ? viewOf.apply(row) : null;
                if (rowView != null) {
                    json.value(rowView.instanceId());
                }
            }
            json.endArray();
            return;
        }

        // A segment (a scope inside this one) is a view of its own: referred to by the id of that view
        if (value instanceof Scope segment) {
            var segmentView = viewOf.apply(segment);
            if (segmentView != null) {
                json.name(field.getName() + "ViewId").value(segmentView.instanceId());
            }
            return;
        }

        if (value instanceof String s) {
            if (!s.isBlank()) {
                json.name(field.getName()).value(s);
            }
            return;
        }

        if (type == int.class) {
            json.name(field.getName()).value((int) value);
            return;
        }
        if (type == long.class) {
            json.name(field.getName()).value((long) value);
            return;
        }
        if (type == double.class) {
            json.name(field.getName()).value((double) value);
            return;
        }
        if (type == float.class) {
            json.name(field.getName()).value((float) value);
            return;
        }
        if (type == short.class) {
            json.name(field.getName()).value((short) value);
            return;
        }
        if (type == byte.class) {
            json.name(field.getName()).value((byte) value);
            return;
        }
        if (type == char.class) {
            json.name(field.getName()).value(String.valueOf((char) value));
            return;
        }

        json.name(field.getName());
        writeValue(value);
    }

    /** Whether the field is a {@code Prop<List<X>>} (or any collection) of a {@link Scope} type: a list of segments. */
    private static boolean isSegmentList(Field field) {
        Type type = field.getGenericType();
        if (type instanceof ParameterizedType prop && prop.getRawType() == Prop.class) {
            type = prop.getActualTypeArguments()[0];
        }
        if (type instanceof ParameterizedType coll && coll.getRawType() instanceof Class<?> raw
                && Collection.class.isAssignableFrom(raw)
                && coll.getActualTypeArguments()[0] instanceof Class<?> element) {
            return Scope.class.isAssignableFrom(element);
        }
        return false;
    }

    private void writeValue(Object value) {
        if (value == null) {
            json.nullValue();
            return;
        }

        if (value instanceof Boolean b) {
            json.value(b);
            return;
        }

        if (value instanceof String s) {
            json.value(s);
            return;
        }

        if (value instanceof Character c) {
            json.value(String.valueOf(c));
            return;
        }

        if (value instanceof java.sql.Date sqlDate) {
            json.value(sqlDate.toLocalDate().format(DATE_FMT));
            return;
        }
        if (value instanceof java.sql.Time sqlTime) {
            json.value(sqlTime.toLocalTime().format(TIME_FMT));
            return;
        }
        if (value instanceof java.util.Date date) {
            json.value(date.toInstant().atOffset(ZoneOffset.UTC).format(DATE_TIME_FMT));
            return;
        }

        if (value instanceof Number n) {
            json.value(n);
            return;
        }

        if (value instanceof Collection<?> coll) {
            json.beginArray();
            for (Object item : coll) {
                writeValue(item);
            }
            json.endArray();
            return;
        }

        if (value instanceof Map<?, ?> map) {
            json.beginObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                json.name(String.valueOf(entry.getKey()));
                writeValue(entry.getValue());
            }
            json.endObject();
            return;
        }

        if (value.getClass().isRecord()) {
            writeRecord(value);
            return;
        }

        Field[] publicFields = value.getClass().getFields();
        if (publicFields.length > 0) {
            writePojo(value, publicFields);
            return;
        }

        json.value(value.toString());
    }

    private void writeRecord(Object rec) {
        try {
            json.beginObject();
            for (var component : rec.getClass().getRecordComponents()) {
                var accessor = component.getAccessor();
                json.name(component.getName());
                writeValue(accessor.invoke(rec));
            }
            json.endObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize record " + rec.getClass().getSimpleName(), e);
        }
    }

    private void writePojo(Object pojo, Field[] fields) {
        try {
            json.beginObject();
            for (Field field : fields) {
                if (!Modifier.isTransient(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())) {
                    Object value = field.get(pojo);
                    if (value != null) {
                        json.name(field.getName());
                        writeValue(value);
                    }
                }
            }
            json.endObject();
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Failed to serialize POJO " + pojo.getClass().getSimpleName(), e);
        }
    }
}
