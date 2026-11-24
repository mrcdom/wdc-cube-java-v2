package br.com.wdc.store.view.teavm.infra;

import java.util.Map;

import org.teavm.jso.JSBody;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * {@link ClientStorage} implementation using the browser's {@code window.localStorage}.
 * <p>
 * Data persists across reloads (F5) and tab/browser closes,
 * until the user clears the browser data or the app removes it explicitly.
 * <p>
 * Each shell gets a short {@code shellId} that prefixes the localStorage keys
 * (e.g. {@code "tw:auth.token"}) to isolate data between shells
 * running on the same origin. IndexedDB (and the AES key) are shared.
 */
public class BrowserLocalStorage implements ClientStorage {

    private final String keyPrefix;

    /**
     * @param shellId short shell identifier (e.g. {@code "tw"}).
     *                Used as the namespace prefix in localStorage.
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
            // The sec.* keys are the encrypted secure view's (EncryptedLocalStorage), not plain values
            if (shortKey.startsWith("sec.")) continue;
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
