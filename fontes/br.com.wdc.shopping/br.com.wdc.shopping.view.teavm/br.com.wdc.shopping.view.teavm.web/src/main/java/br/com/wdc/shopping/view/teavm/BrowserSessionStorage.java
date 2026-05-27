package br.com.wdc.shopping.view.teavm;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * Implementação de {@link ClientStorage} usando {@code window.sessionStorage} do browser.
 * <p>
 * Os dados persistem durante a sessão da aba (sobrevivem ao F5/reload),
 * mas são descartados ao fechar a aba ou o browser.
 */
public class BrowserSessionStorage implements ClientStorage {

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
