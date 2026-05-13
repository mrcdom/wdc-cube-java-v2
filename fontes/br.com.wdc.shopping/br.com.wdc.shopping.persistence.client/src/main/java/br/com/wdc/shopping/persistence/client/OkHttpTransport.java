package br.com.wdc.shopping.persistence.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.wdc.shopping.domain.exception.BusinessException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Implementação de {@link HttpTransport} usando OkHttp.
 */
public class OkHttpTransport implements HttpTransport {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final String baseUrl;
    private final OkHttpClient client;
    private volatile Supplier<String> accessTokenSupplier;

    public OkHttpTransport(String baseUrl) {
        this.baseUrl = baseUrl;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public void setAccessTokenSupplier(Supplier<String> tokenSupplier) {
        this.accessTokenSupplier = tokenSupplier;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    @Override
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

    private void addAuthHeader(Request.Builder builder) {
        var supplier = this.accessTokenSupplier;
        if (supplier != null) {
            var token = supplier.get();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
        }
    }

}
