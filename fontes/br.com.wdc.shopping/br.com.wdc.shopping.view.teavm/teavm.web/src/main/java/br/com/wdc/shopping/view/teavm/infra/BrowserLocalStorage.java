package br.com.wdc.shopping.view.teavm.infra;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * Implementação de {@link ClientStorage} usando {@code window.localStorage} do browser.
 * <p>
 * Os dados persistem entre reloads (F5) e fechamentos de aba/browser,
 * até que o usuário limpe os dados do browser ou o app remova explicitamente.
 * <p>
 * O escopo seguro ({@link #secure()}) usa o prefixo {@code "sec."} no nome das
 * chaves para isolar os valores sensíveis dos demais.
 */
public class BrowserLocalStorage implements ClientStorage {

    private final String keyPrefix;

    public BrowserLocalStorage() {
        this("");
    }

    private BrowserLocalStorage(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * Returns the encrypted view of this storage, backed by {@link EncryptedLocalStorage}.
     * Values are stored with AES-GCM encryption using a non-exportable key from IndexedDB.
     */
    @Override
    public ClientStorage secure() {
        return EncryptedLocalStorage.INSTANCE;
    }

    @Override
    public String get(String key) {
        return jsGet(keyPrefix + key);
    }

    @Override
    public void set(String key, String value) {
        jsSet(keyPrefix + key, value);
    }

    @Override
    public void remove(String key) {
        jsRemove(keyPrefix + key);
    }

    @JSBody(params = "key", script = "try { return localStorage.getItem(key); } catch(e) { return null; }")
    private static native String jsGet(String key);

    @JSBody(params = { "key", "value" }, script = "try { localStorage.setItem(key, value); } catch(e) {}")
    private static native void jsSet(String key, String value);

    @JSBody(params = "key", script = "try { localStorage.removeItem(key); } catch(e) {}")
    private static native void jsRemove(String key);

}
