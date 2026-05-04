package br.com.wdc.shopping.api.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.model.Product;
import br.com.wdc.shopping.domain.model.PurchaseItem;

/**
 * Configuração HTTP compartilhada para os REST repositories.
 * Encapsula {@link HttpTransport} + Gson com adaptador de OffsetDateTime.
 * <p>
 * Suporta autenticação via Bearer token quando um {@link RestAuthClient} está vinculado.
 */
public class RestConfig {

    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private final String baseUrl;
    private final HttpTransport transport;
    private final Gson gson;
    private volatile RestAuthClient authClient;

    /**
     * Cria com transporte OkHttp padrão.
     */
    public RestConfig(String baseUrl) {
        this(baseUrl, new OkHttpTransport());
    }

    /**
     * Cria com transporte customizado (ex: HttpURLConnection para RoboVM).
     */
    public RestConfig(String baseUrl, HttpTransport transport) {
        this.baseUrl = baseUrl;
        this.transport = transport;
        this.gson = new GsonBuilder()
                .registerTypeAdapter(OffsetDateTime.class, new OffsetDateTimeAdapter())
                .setExclusionStrategies(new CircularRefExclusionStrategy())
                .create();
    }

    public Gson gson() {
        return gson;
    }

    public void setAuthClient(RestAuthClient authClient) {
        this.authClient = authClient;
    }

    public RestAuthClient authClient() {
        return authClient;
    }

    public JsonObject postJson(String path, JsonObject body) {
        var headers = jsonHeaders();
        addAuthHeader(headers);

        try {
            var response = transport.execute("POST", baseUrl + path, headers,
                    body.toString().getBytes(StandardCharsets.UTF_8));
            var responseBody = response.bodyAsString();
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    public JsonObject postJsonNullable(String path, JsonObject body) {
        var headers = jsonHeaders();
        addAuthHeader(headers);

        try {
            var response = transport.execute("POST", baseUrl + path, headers,
                    body.toString().getBytes(StandardCharsets.UTF_8));
            if (response.code == 404) {
                return null;
            }
            var responseBody = response.bodyAsString();
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    /**
     * POST sem autenticação (para endpoints públicos como /api/auth/*).
     */
    public JsonObject postJsonPublic(String path, JsonObject body) {
        var headers = jsonHeaders();

        try {
            var response = transport.execute("POST", baseUrl + path, headers,
                    body.toString().getBytes(StandardCharsets.UTF_8));
            var responseBody = response.bodyAsString();
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    /**
     * POST com token explícito (para logout).
     */
    public JsonObject postJsonWithAuth(String path, JsonObject body, String token) {
        var headers = jsonHeaders();
        headers.put("Authorization", "Bearer " + token);

        try {
            var response = transport.execute("POST", baseUrl + path, headers,
                    body.toString().getBytes(StandardCharsets.UTF_8));
            var responseBody = response.bodyAsString();
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    /**
     * GET sem autenticação (para endpoints públicos como /api/auth/challenge).
     */
    public JsonObject getJson(String path) {
        try {
            var response = transport.execute("GET", baseUrl + path, null, null);
            var responseBody = response.bodyAsString();
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("GET " + path, e);
        }
    }

    public byte[] getBytes(String path) {
        var headers = new LinkedHashMap<String, String>();
        addAuthHeader(headers);

        try {
            var response = transport.execute("GET", baseUrl + path, headers, null);
            if (response.code == 404 || response.code == 204) {
                return null;
            }
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code);
            }
            return response.body;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("GET " + path, e);
        }
    }

    public boolean putBytes(String path, byte[] data) {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", OCTET_STREAM_CONTENT_TYPE);
        addAuthHeader(headers);

        try {
            var response = transport.execute("PUT", baseUrl + path, headers, data);
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code);
            }
            var responseBody = response.bodyAsString();
            var json = JsonParser.parseString(responseBody).getAsJsonObject();
            return json.get("success").getAsBoolean();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("PUT " + path, e);
        }
    }

    public void addProjection(JsonObject body, Object projection) {
        if (projection != null) {
            body.add("projection", gson.toJsonTree(projection));
        }
    }

    private Map<String, String> jsonHeaders() {
        var headers = new LinkedHashMap<String, String>();
        headers.put("Content-Type", JSON_CONTENT_TYPE);
        return headers;
    }

    private void addAuthHeader(Map<String, String> headers) {
        if (authClient != null && authClient.accessToken() != null) {
            headers.put("Authorization", "Bearer " + authClient.accessToken());
        }
    }

    private static class OffsetDateTimeAdapter extends TypeAdapter<OffsetDateTime> {
        @Override
        public void write(JsonWriter out, OffsetDateTime value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(value.toString());
            }
        }

        @Override
        public OffsetDateTime read(JsonReader reader) throws IOException {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull();
                return null;
            }
            return Iso8601Util.parseOffsetDateTime(reader.nextString());
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
