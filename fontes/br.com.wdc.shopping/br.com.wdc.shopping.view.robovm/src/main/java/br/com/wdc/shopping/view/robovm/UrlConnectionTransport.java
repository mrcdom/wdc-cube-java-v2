package br.com.wdc.shopping.view.robovm;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

import br.com.wdc.shopping.api.client.HttpTransport;

/**
 * Implementação de {@link HttpTransport} baseada em {@link HttpURLConnection}.
 * Compatível com RoboVM (não depende de OkHttp nem de APIs Java 9+).
 */
public class UrlConnectionTransport implements HttpTransport {

    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 30_000;

    @Override
    public Response execute(String method, String url, Map<String, String> headers, byte[] body) throws IOException {
        @SuppressWarnings("deprecation")
		var connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setUseCaches(false);

            if (headers != null) {
                for (var entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            if (body != null && ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method))) {
                connection.setDoOutput(true);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                    out.flush();
                }
            }

            int code = connection.getResponseCode();
            byte[] responseBody;
            InputStream is = null;
            try {
                is = code >= 400 ? connection.getErrorStream() : connection.getInputStream();
                responseBody = is != null ? readAll(is) : null;
            } finally {
                if (is != null) {
                    try { is.close(); } catch (IOException ignored) {}
                }
            }

            return new Response(code, responseBody);
        } finally {
            connection.disconnect();
        }
    }

    private static byte[] readAll(InputStream is) throws IOException {
        var buffer = new ByteArrayOutputStream(4096);
        var tmp = new byte[4096];
        int n;
        while ((n = is.read(tmp)) != -1) {
            buffer.write(tmp, 0, n);
        }
        return buffer.toByteArray();
    }
}
