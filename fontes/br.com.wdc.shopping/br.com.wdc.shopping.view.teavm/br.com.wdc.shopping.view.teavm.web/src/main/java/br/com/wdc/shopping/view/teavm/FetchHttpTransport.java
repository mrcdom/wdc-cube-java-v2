package br.com.wdc.shopping.view.teavm;

import org.teavm.jso.JSObject;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.api.client.HttpTransport;
import br.com.wdc.shopping.domain.exception.BusinessException;

/**
 * Implementação de {@link HttpTransport} usando a Fetch API do browser.
 * <p>
 * NOTA: No browser, fetch é assíncrono. Aqui usamos uma abordagem síncrona
 * bloqueante via XMLHttpRequest síncrono para manter a interface compatível
 * com o padrão existente (chamadas síncronas dos repositórios).
 * <p>
 * Alternativa futura: migrar para async/await com TeaVM coroutines.
 */
public class FetchHttpTransport implements HttpTransport {

    private final String baseUrl;
    private volatile java.util.function.Supplier<String> accessTokenSupplier;

    public FetchHttpTransport(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public void setAccessTokenSupplier(java.util.function.Supplier<String> tokenSupplier) {
        this.accessTokenSupplier = tokenSupplier;
    }

    @Override
    public JsonObject postJson(String path, JsonObject body) {
        String responseBody = syncFetch(baseUrl + path, "POST", JsonParsing.toJson(body), getToken());
        return JsonParsing.parseObject(responseBody);
    }

    @Override
    public JsonObject postJsonNullable(String path, JsonObject body) {
        SyncResult result = syncFetchRaw(baseUrl + path, "POST", JsonParsing.toJson(body), getToken());
        if (result.status == 404) {
            return null;
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status + ": " + result.body);
        }
        return JsonParsing.parseObject(result.body);
    }

    @Override
    public JsonObject postJsonPublic(String path, JsonObject body) {
        String responseBody = syncFetch(baseUrl + path, "POST", JsonParsing.toJson(body), null);
        return JsonParsing.parseObject(responseBody);
    }

    @Override
    public JsonObject postJsonWithAuth(String path, JsonObject body, String token) {
        String responseBody = syncFetch(baseUrl + path, "POST", JsonParsing.toJson(body), token);
        return JsonParsing.parseObject(responseBody);
    }

    @Override
    public JsonObject getJson(String path) {
        String responseBody = syncFetch(baseUrl + path, "GET", null, null);
        return JsonParsing.parseObject(responseBody);
    }

    @Override
    public byte[] getBytes(String path) {
        SyncResult result = syncFetchRaw(baseUrl + path, "GET", null, getToken());
        if (result.status == 404 || result.status == 204) {
            return null;
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status);
        }
        // Para bytes, usamos Base64 encoding no response ou retornamos como byte[]
        // Simplificação: retornar o body como bytes UTF-8
        return result.body != null ? result.body.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1) : null;
    }

    @Override
    public boolean putBytes(String path, byte[] data) {
        // PUT com content-type octet-stream via XHR síncrono
        SyncResult result = syncXhr(baseUrl + path, "PUT", data, getToken());
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status);
        }
        var json = JsonParsing.parseObject(result.body);
        return json.get("success").getAsBoolean();
    }

    private String getToken() {
        var supplier = accessTokenSupplier;
        return supplier != null ? supplier.get() : null;
    }

    private String syncFetch(String url, String method, String body, String token) {
        SyncResult result = syncFetchRaw(url, method, body, token);
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status + ": " + result.body);
        }
        return result.body;
    }

    /**
     * Executa um XMLHttpRequest síncrono (suportado em browsers, embora deprecated).
     * Necessário para manter interface síncrona dos repositórios.
     */
    private static SyncResult syncFetchRaw(String url, String method, String body, String token) {
        return syncXhr(url, method, body != null ? body.getBytes(java.nio.charset.StandardCharsets.UTF_8) : null, token,
                "application/json");
    }

    private static SyncResult syncXhr(String url, String method, byte[] body, String token) {
        return syncXhr(url, method, body, token, "application/octet-stream");
    }

    @org.teavm.jso.JSBody(params = { "url", "method", "body", "token", "contentType" }, script = ""
            + "var xhr = new XMLHttpRequest();"
            + "xhr.open(method, url, false);" // false = synchronous
            + "xhr.setRequestHeader('Content-Type', contentType);"
            + "if (token) xhr.setRequestHeader('Authorization', 'Bearer ' + token);"
            + "xhr.send(body);"
            + "return { status: xhr.status, body: xhr.responseText };")
    private static native JSObject doSyncXhr(String url, String method, String body, String token, String contentType);

    private static SyncResult syncXhr(String url, String method, byte[] body, String token, String contentType) {
        String bodyStr = body != null ? new String(body, java.nio.charset.StandardCharsets.UTF_8) : null;
        JSObject result = doSyncXhr(url, method, bodyStr, token, contentType);
        int status = getResultStatus(result);
        String responseBody = getResultBody(result);
        return new SyncResult(status, responseBody);
    }

    @org.teavm.jso.JSBody(params = { "result" }, script = "return result.status;")
    private static native int getResultStatus(JSObject result);

    @org.teavm.jso.JSBody(params = { "result" }, script = "return result.body;")
    private static native String getResultBody(JSObject result);

    private static class SyncResult {
        final int status;
        final String body;

        SyncResult(int status, String body) {
            this.status = status;
            this.body = body;
        }
    }

}
