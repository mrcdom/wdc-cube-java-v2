package br.com.wdc.shopping.view.teavm.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;

/**
 * Interop com a Fetch API do browser.
 */
public final class FetchApi {

    private FetchApi() {
    }

    @JSFunctor
    public interface FetchCallback extends JSObject {
        void onComplete(JSObject response);
    }

    @JSFunctor
    public interface ErrorCallback extends JSObject {
        void onError(JSObject error);
    }

    /**
     * Executa um fetch e retorna a resposta como JSON string via callback.
     * Lida com resposta assíncrona e invoca callback na thread principal.
     */
    @JSBody(params = { "url", "method", "body", "authToken", "onSuccess", "onError" }, script = ""
            + "var headers = { 'Content-Type': 'application/json' };"
            + "if (authToken) headers['Authorization'] = 'Bearer ' + authToken;"
            + "var opts = { method: method, headers: headers };"
            + "if (body && method !== 'GET') opts.body = body;"
            + "fetch(url, opts)"
            + "  .then(function(r) { return r.text().then(function(t) { return { status: r.status, body: t }; }); })"
            + "  .then(function(result) { onSuccess(result); })"
            + "  .catch(function(err) { onError(err); });")
    public static native void fetch(String url, String method, String body,
            String authToken, FetchCallback onSuccess, ErrorCallback onError);

    /**
     * Executa um fetch para bytes (GET) e retorna via callback.
     */
    @JSBody(params = { "url", "authToken", "onSuccess", "onError" }, script = ""
            + "var headers = {};"
            + "if (authToken) headers['Authorization'] = 'Bearer ' + authToken;"
            + "fetch(url, { method: 'GET', headers: headers })"
            + "  .then(function(r) {"
            + "    if (!r.ok) { onError({ message: 'HTTP ' + r.status }); return; }"
            + "    return r.arrayBuffer();"
            + "  })"
            + "  .then(function(buf) { if (buf) onSuccess(buf); })"
            + "  .catch(function(err) { onError(err); });")
    public static native void fetchBytes(String url, String authToken,
            FetchCallback onSuccess, ErrorCallback onError);

    /**
     * Obtém o campo 'status' de um objeto de resultado fetch.
     */
    @JSBody(params = { "result" }, script = "return result.status;")
    public static native int getStatus(JSObject result);

    /**
     * Obtém o campo 'body' (string) de um objeto de resultado fetch.
     */
    @JSBody(params = { "result" }, script = "return result.body;")
    public static native String getBody(JSObject result);

    /**
     * Obtém a mensagem de erro.
     */
    @JSBody(params = { "error" }, script = "return error.message || String(error);")
    public static native String getErrorMessage(JSObject error);

}
