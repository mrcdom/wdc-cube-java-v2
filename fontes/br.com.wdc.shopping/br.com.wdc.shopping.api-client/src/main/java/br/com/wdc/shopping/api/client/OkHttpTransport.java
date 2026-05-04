package br.com.wdc.shopping.api.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * Implementação de {@link HttpTransport} baseada em OkHttp.
 * Usada por padrão em ambientes desktop, Android e servidor.
 */
public class OkHttpTransport implements HttpTransport {

    private final OkHttpClient client;

    public OkHttpTransport() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public Response execute(String method, String url, Map<String, String> headers, byte[] body) throws IOException {
        var requestBuilder = new Request.Builder().url(url);

        if (headers != null) {
            for (var entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }
        }

        if ("GET".equalsIgnoreCase(method)) {
            requestBuilder.get();
        } else if ("POST".equalsIgnoreCase(method)) {
            var mediaType = MediaType.parse(getContentType(headers));
            requestBuilder.post(RequestBody.create(body != null ? body : new byte[0], mediaType));
        } else if ("PUT".equalsIgnoreCase(method)) {
            var mediaType = MediaType.parse(getContentType(headers));
            requestBuilder.put(RequestBody.create(body != null ? body : new byte[0], mediaType));
        }

        try (var response = client.newCall(requestBuilder.build()).execute()) {
            var responseBody = response.body() != null ? response.body().bytes() : null;
            return new Response(response.code(), responseBody);
        }
    }

    private static String getContentType(Map<String, String> headers) {
        if (headers != null) {
            for (var entry : headers.entrySet()) {
                if ("Content-Type".equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return "application/json; charset=utf-8";
    }
}
