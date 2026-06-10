package br.com.wdc.shopping.view.teavm.infra;

import java.util.Map;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * Implementação de {@link ClientStorage} usando {@code window.localStorage} do browser.
 * <p>
 * Os dados persistem entre reloads (F5) e fechamentos de aba/browser,
 * até que o usuário limpe os dados do browser ou o app remova explicitamente.
 * <p>
 * Cada shell recebe um {@code shellId} curto que é prefixado nas chaves do
 * localStorage (ex: {@code "tw:auth.token"}) para isolar dados entre shells
 * que rodam na mesma origem. O IndexedDB (e a chave AES) são compartilhados.
 */
public class BrowserLocalStorage implements ClientStorage {

    private final String keyPrefix;

    /**
     * @param shellId identificador curto do shell (ex: {@code "tw"}).
     *                Usado como prefixo de namespace em localStorage.
     */
    public BrowserLocalStorage(String shellId) {
        this.keyPrefix = shellId.isEmpty() ? "" : shellId + ":";
    }

    /**
     * Returns the encrypted view of this storage, backed by {@link EncryptedLocalStorage}.
     * Values are stored with AES-GCM encryption using a non-exportable key from IndexedDB.
     * The prefix used in localStorage is {@code "{shellId}:sec."}.
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

    @Override
    public Map<String, String> all() {
        var result = new java.util.LinkedHashMap<String, String>();
        int len = jsLength();
        for (int i = 0; i < len; i++) {
            String rawKey = jsKey(i);
            if (rawKey == null || !rawKey.startsWith(keyPrefix)) continue;
            String shortKey = rawKey.substring(keyPrefix.length());
            // Only sync keys prefixed with '~'
            if (!shortKey.startsWith("~")) continue;
            String v = jsGet(rawKey);
            if (v != null) result.put(shortKey, v);
        }
        return result;
    }

    @JSBody(params = {}, script = "try { return localStorage.length; } catch(e) { return 0; }")
    private static native int jsLength();

    @JSBody(params = {"i"}, script = "try { return localStorage.key(i); } catch(e) { return null; }")
    private static native String jsKey(int i);

    @JSBody(params = "key", script = "try { return localStorage.getItem(key); } catch(e) { return null; }")
    private static native String jsGet(String key);

    @JSBody(params = { "key", "value" }, script = "try { localStorage.setItem(key, value); } catch(e) {}")
    private static native void jsSet(String key, String value);

    @JSBody(params = "key", script = "try { localStorage.removeItem(key); } catch(e) {}")
    private static native void jsRemove(String key);

}
