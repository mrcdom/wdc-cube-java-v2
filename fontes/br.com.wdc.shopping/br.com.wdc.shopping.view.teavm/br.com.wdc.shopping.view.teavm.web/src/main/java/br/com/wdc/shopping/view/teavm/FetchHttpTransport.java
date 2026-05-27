package br.com.wdc.shopping.view.teavm;

import org.teavm.jso.JSObject;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.shopping.persistence.client.HttpTransport;
import br.com.wdc.shopping.domain.exception.BusinessException;

/**
 * Implementação de {@link HttpTransport} usando XMLHttpRequest síncrono do browser.
 * <p>
 * Retorna JSON como String bruta, compatível com JsonStreamReader para parsing.
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
    public String postJson(String path, String body) {
        return syncFetch(baseUrl + path, "POST", body, getToken());
    }

    @Override
    public String postJsonNullable(String path, String body) {
        String token = getToken();
        SyncResult result = syncFetchRaw(baseUrl + path, "POST", body, token);
        if (result.status == 401 && token != null) {
            handleSessionExpired();
        }
        if (result.status == 404) {
            return null;
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status + ": " + result.body);
        }
        return result.body;
    }

    @Override
    public String postJsonPublic(String path, String body) {
        return syncFetch(baseUrl + path, "POST", body, null);
    }

    @Override
    public String postJsonWithAuth(String path, String body, String token) {
        return syncFetch(baseUrl + path, "POST", body, token);
    }

    @Override
    public String getJson(String path) {
        return syncFetch(baseUrl + path, "GET", null, null);
    }

    @Override
    public byte[] getBytes(String path) {
        String token = getToken();
        SyncResult result = syncFetchRaw(baseUrl + path, "GET", null, token);
        if (result.status == 401 && token != null) {
            handleSessionExpired();
        }
        if (result.status == 404 || result.status == 204) {
            return null;
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status);
        }
        return result.body != null ? result.body.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1) : null;
    }

    @Override
    public boolean putBytes(String path, byte[] data) {
        String token = getToken();
        SyncResult result = syncXhr(baseUrl + path, "PUT", data, token);
        if (result.status == 401 && token != null) {
            handleSessionExpired();
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status);
        }
        var reader = new JsonStreamReader(result.body);
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
    }

    private String getToken() {
        var supplier = accessTokenSupplier;
        return supplier != null ? supplier.get() : null;
    }

    private String syncFetch(String url, String method, String body, String token) {
        SyncResult result = syncFetchRaw(url, method, body, token);
        if (result.status == 401 && token != null) {
            handleSessionExpired();
        }
        if (result.status < 200 || result.status >= 300) {
            throw new BusinessException("HTTP " + result.status + ": " + result.body);
        }
        return result.body;
    }

    /**
     * Exibe diálogo de sessão expirada e recarrega a página (volta ao login).
     */
    private static void handleSessionExpired() {
        showSessionExpiredDialog();
        reloadPage();
        throw new BusinessException("Sessão expirada");
    }

    @org.teavm.jso.JSBody(params = {}, script = ""
            + "alert('Sua sessão no servidor foi encerrada.\\nVocê será redirecionado ao login.');")
    private static native void showSessionExpiredDialog();

    @org.teavm.jso.JSBody(params = {}, script = "window.location.reload();")
    private static native void reloadPage();

    private static SyncResult syncFetchRaw(String url, String method, String body, String token) {
        return syncXhr(url, method, body != null ? body.getBytes(java.nio.charset.StandardCharsets.UTF_8) : null, token,
                "application/json");
    }

    private static SyncResult syncXhr(String url, String method, byte[] body, String token) {
        return syncXhr(url, method, body, token, "application/octet-stream");
    }

    @org.teavm.jso.JSBody(params = { "url", "method", "body", "token", "contentType" }, script = ""
            + "var xhr = new XMLHttpRequest();"
            + "xhr.open(method, url, false);"
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
