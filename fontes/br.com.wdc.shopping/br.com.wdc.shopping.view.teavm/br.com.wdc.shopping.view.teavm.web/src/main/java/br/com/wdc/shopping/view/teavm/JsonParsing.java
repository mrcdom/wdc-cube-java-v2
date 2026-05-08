package br.com.wdc.shopping.view.teavm;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * Manual JSON parser/writer using Gson's streaming API.
 * Avoids triggering TypeAdapters.&lt;clinit&gt; which uses AtomicIntegerArray.
 */
final class JsonParsing {

    private JsonParsing() {}

    // :: Parsing

    static JsonObject parseObject(String json) {
        try (var reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.LENIENT);
            return readObject(reader);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON: " + e.getMessage(), e);
        }
    }

    // :: Writing

    static String toJson(JsonElement element) {
        try {
            var sw = new StringWriter();
            var writer = new JsonWriter(sw);
            writer.setStrictness(Strictness.LENIENT);
            writeElement(writer, element);
            writer.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to write JSON: " + e.getMessage(), e);
        }
    }

    private static void writeElement(JsonWriter writer, JsonElement element) throws Exception {
        if (element == null || element.isJsonNull()) {
            writer.nullValue();
        } else if (element.isJsonPrimitive()) {
            var p = element.getAsJsonPrimitive();
            if (p.isBoolean()) {
                writer.value(p.getAsBoolean());
            } else if (p.isNumber()) {
                writer.value(p.getAsNumber());
            } else {
                writer.value(p.getAsString());
            }
        } else if (element.isJsonObject()) {
            writer.beginObject();
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                writer.name(entry.getKey());
                writeElement(writer, entry.getValue());
            }
            writer.endObject();
        } else if (element.isJsonArray()) {
            writer.beginArray();
            for (JsonElement item : element.getAsJsonArray()) {
                writeElement(writer, item);
            }
            writer.endArray();
        }
    }

    private static JsonElement readElement(JsonReader reader) throws Exception {
        JsonToken token = reader.peek();
        return switch (token) {
            case BEGIN_OBJECT -> readObject(reader);
            case BEGIN_ARRAY -> readArray(reader);
            case STRING -> new JsonPrimitive(reader.nextString());
            case NUMBER -> new JsonPrimitive(reader.nextDouble());
            case BOOLEAN -> new JsonPrimitive(reader.nextBoolean());
            case NULL -> { reader.nextNull(); yield JsonNull.INSTANCE; }
            default -> throw new IllegalStateException("Unexpected token: " + token);
        };
    }

    private static JsonObject readObject(JsonReader reader) throws Exception {
        var obj = new JsonObject();
        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();
            obj.add(name, readElement(reader));
        }
        reader.endObject();
        return obj;
    }

    private static JsonArray readArray(JsonReader reader) throws Exception {
        var arr = new JsonArray();
        reader.beginArray();
        while (reader.hasNext()) {
            arr.add(readElement(reader));
        }
        reader.endArray();
        return arr;
    }
}
