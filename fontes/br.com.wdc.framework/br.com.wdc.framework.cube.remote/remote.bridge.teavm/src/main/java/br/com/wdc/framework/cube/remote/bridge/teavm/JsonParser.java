package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.commons.serialization.SerializationToken;

/**
 * JSON parser/serializer for WebSocket messages.
 * Uses {@link JsonStreamReader}/{@link JsonStreamWriter} (pure Java) to preserve
 * type semantics: integers stay as Long/Integer, floats as Double.
 */
public final class JsonParser {

    private JsonParser() {
    }

    public static Map<String, Object> parseObject(String json) {
        if (json == null || json.isEmpty()) return Map.of();
        try {
            var reader = new JsonStreamReader(json);
            if (reader.peek() != SerializationToken.BEGIN_OBJECT) return Map.of();
            return readObject(reader);
        } catch (Exception e) {
            return Map.of();
        }
    }

    public static String stringify(Object obj) {
        var writer = new JsonStreamWriter();
        writeValue(writer, obj);
        return writer.result();
    }

    // ── Reading ──

    private static Map<String, Object> readObject(JsonStreamReader reader) {
        var map = new LinkedHashMap<String, Object>();
        reader.beginObject();
        while (reader.hasNext()) {
            var name = reader.nextName();
            map.put(name, readValue(reader));
        }
        reader.endObject();
        return map;
    }

    private static List<Object> readArray(JsonStreamReader reader) {
        var list = new ArrayList<Object>();
        reader.beginArray();
        while (reader.hasNext()) {
            list.add(readValue(reader));
        }
        reader.endArray();
        return list;
    }

    private static Object readValue(JsonStreamReader reader) {
        return switch (reader.peek()) {
            case BEGIN_OBJECT -> readObject(reader);
            case BEGIN_ARRAY -> readArray(reader);
            case STRING -> reader.nextString();
            case NUMBER -> reader.nextNumber();
            case BOOLEAN -> reader.nextBoolean();
            case NULL -> reader.nextNull();
            default -> { reader.skipValue(); yield null; }
        };
    }

    // ── Writing ──

    @SuppressWarnings("unchecked")
    private static void writeValue(JsonStreamWriter writer, Object obj) {
        if (obj == null) {
            writer.nullValue();
        } else if (obj instanceof String s) {
            writer.value(s);
        } else if (obj instanceof Integer i) {
            writer.value((long) i);
        } else if (obj instanceof Long l) {
            writer.value(l);
        } else if (obj instanceof Double d) {
            writer.value(d);
        } else if (obj instanceof Number n) {
            writer.value(n.doubleValue());
        } else if (obj instanceof Boolean b) {
            writer.value(b);
        } else if (obj instanceof Map<?, ?> map) {
            writer.beginObject();
            for (var entry : ((Map<String, Object>) map).entrySet()) {
                writer.name(entry.getKey());
                writeValue(writer, entry.getValue());
            }
            writer.endObject();
        } else if (obj instanceof List<?> list) {
            writer.beginArray();
            for (var item : list) {
                writeValue(writer, item);
            }
            writer.endArray();
        } else {
            writer.value(obj.toString());
        }
    }
}
