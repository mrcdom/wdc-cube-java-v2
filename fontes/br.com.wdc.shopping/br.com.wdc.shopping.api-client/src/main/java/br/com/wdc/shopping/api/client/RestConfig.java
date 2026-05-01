package br.com.wdc.shopping.api.client;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

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
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Configuração HTTP compartilhada para os REST repositories.
 * Encapsula OkHttpClient + Gson com adaptador de OffsetDateTime.
 * <p>
 * Suporta autenticação via Bearer token quando um {@link RestAuthClient} está vinculado.
 */
public class RestConfig {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient client;
    private final Gson gson;
    private volatile RestAuthClient authClient;

    public RestConfig(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
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
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE));
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    public JsonObject postJsonNullable(String path, JsonObject body) {
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE));
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            if (response.code() == 404) {
                return null;
            }
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
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
        var request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
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
        var request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body.toString(), JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer " + token)
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
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
        var request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return JsonParser.parseString(responseBody).getAsJsonObject();
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("GET " + path, e);
        }
    }

    public byte[] getBytes(String path) {
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .get();
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            if (response.code() == 404 || response.code() == 204) {
                return null;
            }
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code());
            }
            return response.body() != null ? response.body().bytes() : null;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("GET " + path, e);
        }
    }

    public boolean putBytes(String path, byte[] data) {
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .put(RequestBody.create(data, MediaType.parse("application/octet-stream")));
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code());
            }
            var responseBody = response.body() != null ? response.body().string() : null;
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

    private void addAuthHeader(Request.Builder builder) {
        if (authClient != null && authClient.accessToken() != null) {
            builder.header("Authorization", "Bearer " + authClient.accessToken());
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
