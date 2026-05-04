package br.com.wdc.shopping.api.client;

import java.io.IOException;
import java.util.Map;

/**
 * Abstração de transporte HTTP para desacoplar o módulo api-client
 * de uma implementação específica (OkHttp, HttpURLConnection, etc.).
 */
public interface HttpTransport {

    /**
     * Resultado de uma requisição HTTP.
     */
    final class Response {
        public final int code;
        public final byte[] body;

        public Response(int code, byte[] body) {
            this.code = code;
            this.body = body;
        }

        public boolean isSuccessful() {
            return code >= 200 && code < 300;
        }

        public String bodyAsString() {
            return body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
        }
    }

    Response execute(String method, String url, Map<String, String> headers, byte[] body) throws IOException;
}
