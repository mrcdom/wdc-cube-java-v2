package br.com.wdc.shopping.api.client;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.PurchaseItem;

/**
 * Configuração compartilhada para os REST repositories.
 * Encapsula {@link HttpTransport} + Gson com adaptador de OffsetDateTime.
 * <p>
 * O transporte HTTP é fornecido externamente via construtor, permitindo
 * diferentes implementações (OkHttp para JVM, fetch para browser/TeaVM, etc.).
 */
public class RestConfig {

    private final HttpTransport transport;
    private final Gson gson;
    private volatile RestAuthClient authClient;

    public RestConfig(HttpTransport transport) {
        this.transport = transport;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .setExclusionStrategies(new CircularRefExclusionStrategy())
                .create();
    }

    public Gson gson() {
        return gson;
    }

    public HttpTransport transport() {
        return transport;
    }

    public void setAuthClient(RestAuthClient authClient) {
        this.authClient = authClient;
        transport.setAccessTokenSupplier(authClient != null ? authClient::accessToken : null);
    }

    public RestAuthClient authClient() {
        return authClient;
    }

    public JsonObject postJson(String path, JsonObject body) {
        return transport.postJson(path, body);
    }

    public JsonObject postJsonNullable(String path, JsonObject body) {
        return transport.postJsonNullable(path, body);
    }

    public JsonObject postJsonPublic(String path, JsonObject body) {
        return transport.postJsonPublic(path, body);
    }

    public JsonObject postJsonWithAuth(String path, JsonObject body, String token) {
        return transport.postJsonWithAuth(path, body, token);
    }

    public JsonObject getJson(String path) {
        return transport.getJson(path);
    }

    public byte[] getBytes(String path) {
        return transport.getBytes(path);
    }

    public boolean putBytes(String path, byte[] data) {
        return transport.putBytes(path, data);
    }

    public void addProjection(JsonObject body, Object projection) {
        if (projection != null) {
            body.add("projection", gson.toJsonTree(projection));
        }
    }

    private static class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {
        @Override
        public void write(JsonWriter out, OffsetDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            }
        }

        @Override
        public OffsetDateTime read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            return OffsetDateTime.parse(reader.nextString(), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }
    }

    private static class CircularRefExclusionStrategy implements ExclusionStrategy {
        @Override
        public boolean shouldSkipField(FieldAttributes f) {
            if (f.getDeclaringClass() == PurchaseItem.class && "purchase".equals(f.getName())) return true;
            if (f.getDeclaringClass() == Product.class && "image".equals(f.getName())) return true;
            return false;
        }

        @Override
        public boolean shouldSkipClass(Class<?> clazz) {
            return false;
        }
    }
}
