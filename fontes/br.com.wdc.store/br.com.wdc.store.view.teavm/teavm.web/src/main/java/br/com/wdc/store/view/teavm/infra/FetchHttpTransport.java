package br.com.wdc.store.view.teavm.infra;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.JSBody;
import org.teavm.jso.ajax.XMLHttpRequest;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.persistence.client.HttpFailure;
import br.com.wdc.store.view.teavm.commons.I18n;

/**
 * {@link HttpTransport} implementation using asynchronous XMLHttpRequest through TeaVM coroutines.
 * <p>
 * Uses {@code @Async} + {@link AsyncCallback} to suspend the coroutine while
 * waiting for the HTTP response, avoiding blocking the browser's main thread.
 * The Java API stays synchronous (transparent to the caller).
 */
public class FetchHttpTransport implements HttpTransport {

	private final String baseUrl;
	private final AtomicReference<Supplier<String>> accessTokenSupplier = new AtomicReference<>();
	private final AtomicReference<Supplier<String>> txIdSupplier = new AtomicReference<>();

	public FetchHttpTransport(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	@Override
	public void setAccessTokenSupplier(Supplier<String> tokenSupplier) {
		this.accessTokenSupplier.set(tokenSupplier);
	}

	@Override
	public void setTransactionIdSupplier(Supplier<String> txIdSupplier) {
		this.txIdSupplier.set(txIdSupplier);
	}

	@Override
	public String postJson(String path, String body) {
		return doFetch(baseUrl + path, "POST", body, getToken(), getTxId());
	}

	@Override
	public String postJsonNullable(String path, String body) {
		String token = getToken();
		HttpResult result = asyncXhr(baseUrl + path, "POST", body, token, "application/json", getTxId());
		if (result.status == 0) {
			handleConnectionRefused();
		}
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status == 404) {
			return null;
		}
		if (result.status < 200 || result.status >= 300) {
			throw HttpFailure.of(result.status, result.body);
		}
		return result.body;
	}

	@Override
	public String postJsonPublic(String path, String body) {
		return doFetch(baseUrl + path, "POST", body, null, null);
	}

	@Override
	public String postJsonWithAuth(String path, String body, String token) {
		return doFetch(baseUrl + path, "POST", body, token, null);
	}

	@Override
	public String getJson(String path) {
		return doFetch(baseUrl + path, "GET", null, null, null);
	}

	@Override
	public byte[] getBytes(String path) {
		String token = getToken();
		HttpResult result = asyncXhr(baseUrl + path, "GET", null, token, "application/json", getTxId());
		if (result.status == 0) {
			handleConnectionRefused();
		}
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status == 404 || result.status == 204) {
			return new byte[0];
		}
		if (result.status < 200 || result.status >= 300) {
			throw HttpFailure.of(result.status, null);
		}
		return result.body != null ? result.body.getBytes(StandardCharsets.ISO_8859_1) : new byte[0];
	}

	@Override
	public boolean putBytes(String path, byte[] data) {
		String token = getToken();
		String bodyStr = data != null ? new String(data, StandardCharsets.UTF_8) : null;
		HttpResult result = asyncXhr(baseUrl + path, "PUT", bodyStr, token, "application/octet-stream", getTxId());
		if (result.status == 0) {
			handleConnectionRefused();
		}
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status < 200 || result.status >= 300) {
			throw HttpFailure.of(result.status, null);
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
		var supplier = accessTokenSupplier.get();
		return supplier != null ? supplier.get() : null;
	}

	/**
	 * This tab's id, sent in X-Client-Id: the owner, with the user, of the tab's remote transactions. Kept in the tab's
	 * sessionStorage, so a reload keeps it and another tab has its own.
	 */
	private static String clientId;

	private static String clientId() {
		if (clientId == null) {
			clientId = tabClientId();
		}
		return clientId;
	}

	/**
	 * The tab's id from sessionStorage, or a new random UUID (v4) stored there. crypto.getRandomValues works outside a
	 * secure context, unlike crypto.randomUUID (plain HTTP away from localhost); Math.random only without crypto at all.
	 * Without sessionStorage (blocked), the id lasts as long as the page.
	 */
	@JSBody(params = {}, script = ""
			+ "var key = 'wdc.clientId', id = null;"
			+ "try { id = sessionStorage.getItem(key); } catch (e) {}"
			+ "if (id) { return id; }"
			+ "var b = new Uint8Array(16);"
			+ "if (self.crypto && self.crypto.getRandomValues) { self.crypto.getRandomValues(b); }"
			+ "else { for (var i = 0; i < 16; i++) { b[i] = Math.random() * 256 | 0; } }"
			+ "b[6] = (b[6] & 0x0f) | 0x40; b[8] = (b[8] & 0x3f) | 0x80;"
			+ "var h = []; for (var j = 0; j < 16; j++) { h.push((b[j] + 0x100).toString(16).substring(1)); }"
			+ "id = h.slice(0, 4).join('') + '-' + h.slice(4, 6).join('') + '-' + h.slice(6, 8).join('') + '-'"
			+ "    + h.slice(8, 10).join('') + '-' + h.slice(10, 16).join('');"
			+ "try { sessionStorage.setItem(key, id); } catch (e) {}"
			+ "return id;")
	private static native String tabClientId();

	private String getTxId() {
		var supplier = txIdSupplier.get();
		return supplier != null ? supplier.get() : null;
	}

	private String doFetch(String url, String method, String body, String token, String txId) {
		HttpResult result = asyncXhr(url, method, body, token, "application/json", txId);
		if (result.status == 0) {
			handleConnectionRefused();
		}
		if (result.status == 401 && token != null) {
			handleSessionExpired();
		}
		if (result.status < 200 || result.status >= 300) {
			throw HttpFailure.of(result.status, result.body);
		}
		return result.body;
	}

	// ── Coroutine-based async XHR ───────────────────────────────────────────

	/**
	 * Runs XMLHttpRequest asynchronously using TeaVM coroutines.
	 * The method looks synchronous to the Java caller, but internally
	 * suspends the coroutine until the callback is invoked.
	 */
	@Async
	private static native HttpResult asyncXhr(String url, String method, String body, String token, String contentType,
			String txId);

	@SuppressWarnings("java:S1144") // Called by TeaVM @Async infrastructure
	private static void asyncXhr(String url, String method, String body, String token, String contentType, String txId,
			AsyncCallback<HttpResult> callback) {
		XMLHttpRequest xhr = new XMLHttpRequest();
		xhr.open(method, url);
		xhr.setRequestHeader("Content-Type", contentType);
		xhr.setRequestHeader("X-Client-Id", clientId());
		if (token != null) {
			xhr.setRequestHeader("Authorization", "Bearer " + token);
		}
		if (txId != null) {
			xhr.setRequestHeader("X-Tx-Id", txId);
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

	private static void handleConnectionRefused() {
		showDialog(I18n.text(StoreMessages.ERROR_CONNECTION));
		throw new BusinessException("Server unavailable");
	}

	private static void handleSessionExpired() {
		showDialog(I18n.text(StoreMessages.TEAVM_SESSION_ENDED));
		reloadPage();
		throw new BusinessException("Session expired");
	}

	@JSBody(params = { "message" }, script = "alert(message);")
	private static native void showDialog(String message);

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
