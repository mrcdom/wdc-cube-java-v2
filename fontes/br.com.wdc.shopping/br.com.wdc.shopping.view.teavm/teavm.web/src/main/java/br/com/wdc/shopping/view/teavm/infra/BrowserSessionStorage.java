package br.com.wdc.shopping.view.teavm.infra;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * {@link ClientStorage} implementation using the browser's {@code window.sessionStorage}.
 * <p>
 * Data persists during the tab session (survives F5/reload),
 * but is discarded when the tab or the browser is closed.
 */
public class BrowserSessionStorage implements ClientStorage {

	@Override
	public ClientStorage secure() {
		return this; // the browser's sessionStorage is ephemeral — no extra secure backing
	}

	@Override
	public String get(String key) {
		return jsGet(key);
	}

	@Override
	public void set(String key, String value) {
		jsSet(key, value);
	}

	@Override
	public void remove(String key) {
		jsRemove(key);
	}

	@JSBody(params = "key", script = "return sessionStorage.getItem(key);")
	private static native String jsGet(String key);

	@JSBody(params = { "key", "value" }, script = "sessionStorage.setItem(key, value);")
	private static native void jsSet(String key, String value);

	@JSBody(params = "key", script = "sessionStorage.removeItem(key);")
	private static native void jsRemove(String key);

}
