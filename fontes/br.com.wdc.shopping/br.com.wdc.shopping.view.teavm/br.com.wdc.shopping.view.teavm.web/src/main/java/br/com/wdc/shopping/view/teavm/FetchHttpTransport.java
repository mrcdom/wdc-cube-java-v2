package br.com.wdc.shopping.view.teavm;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.shopping.domain.exception.BusinessException;

/**
 * Implementação de {@link HttpTransport} usando XMLHttpRequest assíncrono via coroutines do TeaVM.
 * <p>
 * Usa {@code @Async} + {@link AsyncCallback} para suspender a coroutine enquanto
 * aguarda a resposta HTTP, evitando bloqueio da thread principal do browser.
 * A API Java permanece síncrona (transparente para o chamador).
 */
public class FetchHttpTransport implements HttpTransport {

	private final String baseUrl;
	private volatile Supplier<String> accessTokenSupplier;

	public FetchHttpTransport(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public void setAccessTokenSupplier(Supplier<String> tokenSupplier) {
		this.accessTokenSupplier = tokenSupplier;
	}

	@Override
	public String postJson(String path, String body) {
		return doFetch(baseUrl + path, "POST", body, getToken());
	}

	@Override
	public String postJsonNullable(String path, String body) {
		String token = getToken();
		HttpResult result = asyncXhr(baseUrl + path, "POST", body, token, "application/json");
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
		return doFetch(baseUrl + path, "POST", body, null);
	}

	@Override
	public String postJsonWithAuth(String path, String body, String token) {
		return doFetch(baseUrl + path, "POST", body, token);
	}

	@Override
	public String getJson(String path) {
		return doFetch(baseUrl + path, "GET", null, null);
	}

	@Override
	public byte[] getBytes(String path) {
		String token = getToken();
		HttpResult result = asyncXhr(baseUrl + path, "GET", null, token, "application/json");
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status == 404 || result.status == 204) {
			return null;
		}
		if (result.status < 200 || result.status >= 300) {
			throw new BusinessException("HTTP " + result.status);
		}
		return result.body != null ? result.body.getBytes(StandardCharsets.ISO_8859_1) : null;
	}

	@Override
	public boolean putBytes(String path, byte[] data) {
		String token = getToken();
		String bodyStr = data != null ? new String(data, StandardCharsets.UTF_8) : null;
		HttpResult result = asyncXhr(baseUrl + path, "PUT", bodyStr, token, "application/octet-stream");
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

	// ── Private helpers ──────────────────────────────────────────────────────

	private String getToken() {
		var supplier = accessTokenSupplier;
		return supplier != null ? supplier.get() : null;
	}

	private String doFetch(String url, String method, String body, String token) {
		HttpResult result = asyncXhr(url, method, body, token, "application/json");
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status < 200 || result.status >= 300) {
			throw new BusinessException("HTTP " + result.status + ": " + result.body);
		}
		return result.body;
	}

	// ── Coroutine-based async XHR ───────────────────────────────────────────

	/**
	 * Executa XMLHttpRequest de forma assíncrona usando coroutines do TeaVM.
	 * O método aparenta ser síncrono para o chamador Java, mas internamente
	 * suspende a coroutine até o callback ser invocado.
	 */
	@Async
	private static native HttpResult asyncXhr(String url, String method, String body, String token, String contentType);

	private static void asyncXhr(String url, String method, String body, String token, String contentType,
			AsyncCallback<HttpResult> callback) {
		XMLHttpRequest xhr = new XMLHttpRequest();
		xhr.open(method, url);
		xhr.setRequestHeader("Content-Type", contentType);
		if (token != null) {
			xhr.setRequestHeader("Authorization", "Bearer " + token);
		}
		xhr.setOnReadyStateChange(() -> {
			if (xhr.getReadyState() != XMLHttpRequest.DONE) {
				return;
			}
			callback.complete(new HttpResult(xhr.getStatus(), xhr.getResponseText()));
		});
		if (body != null) {
			xhr.send(body);
		} else {
			xhr.send();
		}
	}

	// ── Session expired handling ─────────────────────────────────────────────

	private static void handleSessionExpired() {
		showSessionExpiredDialog();
		reloadPage();
		throw new BusinessException("Sessão expirada");
	}

	@JSBody(params = {}, script = "alert('Sua sessão no servidor foi encerrada.\\nVocê será redirecionado ao login.');")
	private static native void showSessionExpiredDialog();

	@JSBody(params = {}, script = "window.location.reload();")
	private static native void reloadPage();

	// ── Result holder ────────────────────────────────────────────────────────

	private static class HttpResult {
		final int status;
		final String body;

		HttpResult(int status, String body) {
			this.status = status;
			this.body = body;
		}
	}

}
