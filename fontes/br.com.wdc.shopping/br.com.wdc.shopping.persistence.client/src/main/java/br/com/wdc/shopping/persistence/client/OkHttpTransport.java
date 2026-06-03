package br.com.wdc.shopping.persistence.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
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
    private final AtomicReference<Supplier<String>> accessTokenSupplier = new AtomicReference<>();

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
        this.accessTokenSupplier.set(tokenSupplier);
    }

    @Override
    public String postJson(String path, String body) {
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE));
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    @Override
    public String postJsonNullable(String path, String body) {
        var requestBuilder = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE));
        addAuthHeader(requestBuilder);

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            if (response.code() == 404) {
                return null;
            }
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    @Override
    public String postJsonPublic(String path, String body) {
        var request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE))
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    @Override
    public String postJsonWithAuth(String path, String body, String token) {
        var request = new Request.Builder()
                .url(baseUrl + path)
                .post(RequestBody.create(body, JSON_MEDIA_TYPE))
                .header("Authorization", "Bearer " + token)
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("POST " + path, e);
        }
    }

    @Override
    public String getJson(String path) {
        var request = new Request.Builder()
                .url(baseUrl + path)
                .get()
                .build();

        try (var response = client.newCall(request).execute()) {
            var responseBody = response.body() != null ? response.body().string() : null;
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code() + ": " + responseBody);
            }
            return responseBody;
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
                return null; // NOSONAR S1168 — null means "resource not found", distinct from empty
            }
            if (!response.isSuccessful()) {
                throw new BusinessException("HTTP " + response.code());
            }
            return response.body() != null ? response.body().bytes() : new byte[0];
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
            if (responseBody == null) return false;
            var reader = new JsonStreamReader(responseBody);
            reader.beginObject();
            boolean success = false;
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "success" -> success = Boolean.TRUE.equals(InputCoerceUtils.asBoolean(reader));
                    default -> reader.skipValue();
                }
            }
            reader.endObject();
            return success;
        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            throw BusinessException.wrap("PUT " + path, e);
        }
    }

    private void addAuthHeader(Request.Builder builder) {
        var supplier = this.accessTokenSupplier.get();
        if (supplier != null) {
            var token = supplier.get();
            if (token != null) {
                builder.header("Authorization", "Bearer " + token);
            }
        }
    }

}
