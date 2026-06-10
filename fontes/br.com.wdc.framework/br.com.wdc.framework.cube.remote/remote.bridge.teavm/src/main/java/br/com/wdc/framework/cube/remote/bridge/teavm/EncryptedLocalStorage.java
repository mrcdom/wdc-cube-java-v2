package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Console;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsObjectConsumer;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;

/**
 * AES-GCM encrypted {@link ClientStorage} for the TeaVM remote shell.
 * <p>
 * The AES-256 key is generated with {@code extractable: false} and persisted in
 * the origin's IndexedDB ({@code _sec} database, object store {@code k}, key
 * {@code 0}). Because the key is non-extractable, its raw bytes are never
 * accessible to JavaScript — not even via {@code crypto.subtle.exportKey()}.
 * <p>
 * Values are stored in {@code localStorage} under a {@code sec.} prefix as
 * {@code base64(iv[12] || ciphertext)}. A fresh random IV is generated per
 * write.
 * <p>
 * Usage: call {@link #initialize(Runnable)} once at startup (before
 * {@code app.start()}). After the callback fires the cache is fully populated
 * and all read/write operations are synchronous (cache-backed).
 * <p>
 * Fallback: if IndexedDB is unavailable (some private-browsing modes), the
 * storage works in-memory only — data is not persisted across reloads.
 */
public final class EncryptedLocalStorage implements ClientStorage {

    /** Singleton — shared between {@link ViewStateCoordinator} and startup. */
    public static final EncryptedLocalStorage INSTANCE = new EncryptedLocalStorage();

    private static final String LS_PREFIX = "sec.";

    private final Map<String, String> cache = new LinkedHashMap<>();

    /** Non-null once {@link #initialize} completes successfully. */
    private JSObject cryptoKey = null;

    private EncryptedLocalStorage() {
    }

    // -----------------------------------------------------------------------
    // ClientStorage interface
    // -----------------------------------------------------------------------

    @Override
    public ClientStorage secure() {
        return this;
    }

    @Override
    public String get(String key) {
        return cache.get(key);
    }

    @Override
    public void set(String key, String value) {
        cache.put(key, value);
        if (cryptoKey != null) {
            var k = cryptoKey;
            encryptValue(k, value, encrypted -> {
                if (!encrypted.isEmpty()) {
                    lsSetItem(LS_PREFIX + key, encrypted);
                }
            });
        }
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
        lsRemoveItem(LS_PREFIX + key);
    }

    @Override
    public Map<String, String> all() {
        return new LinkedHashMap<>(cache);
    }

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    /**
     * Opens (or creates) the IndexedDB AES key, then pre-decrypts all
     * {@code sec.*} localStorage entries into the in-memory cache.
     * Calls {@code onReady} when the cache is fully populated.
     * <p>
     * If IndexedDB is unavailable, {@code onReady} is called immediately and
     * the storage works in-memory only.
     *
     * @param onReady called once initialization is complete
     */
    public static void initialize(Runnable onReady) {
        openOrCreateKey(key -> {
            INSTANCE.cryptoKey = key;
            decryptAll(key, onReady);
        });
    }

    private static void decryptAll(JSObject key, Runnable onReady) {
        int len = lsLength();

        // Collect all sec.* pairs first (avoid re-reading while iterating)
        var pairs = new ArrayList<String[]>();
        for (int i = 0; i < len; i++) {
            String rawKey = lsKey(i);
            if (rawKey == null || !rawKey.startsWith(LS_PREFIX)) continue;
            String b64 = lsGetItem(rawKey);
            if (b64 != null && !b64.isEmpty()) {
                pairs.add(new String[]{rawKey.substring(LS_PREFIX.length()), b64});
            }
        }

        if (pairs.isEmpty()) {
            onReady.run();
            return;
        }

        int[] remaining = {pairs.size()};
        for (var pair : pairs) {
            String shortKey = pair[0];
            String b64 = pair[1];
            decryptValue(key, b64, plaintext -> {
                if (plaintext != null && !plaintext.isEmpty()) {
                    INSTANCE.cache.put(shortKey, plaintext);
                }
                remaining[0]--;
                if (remaining[0] == 0) {
                    onReady.run();
                }
            });
        }
    }

    // -----------------------------------------------------------------------
    // IndexedDB — open or create the non-extractable AES key
    // -----------------------------------------------------------------------

    @JSBody(params = {"callback"}, script = """
            var req = indexedDB.open('_sec', 1);
            req.onupgradeneeded = function(e) {
                e.target.result.createObjectStore('k');
            };
            req.onerror = function() {
                console.warn('[EncryptedLocalStorage] IndexedDB unavailable');
                // fall back: pass a null-like sentinel
                callback({_idbFailed: true});
            };
            req.onsuccess = function() {
                var db = req.result;
                var tx = db.transaction('k', 'readwrite');
                var store = tx.objectStore('k');
                var getReq = store.get(0);
                getReq.onsuccess = function() {
                    if (getReq.result) {
                        callback(getReq.result);
                        return;
                    }
                    crypto.subtle.generateKey(
                        { name: 'AES-GCM', length: 256 }, false,
                        ['encrypt', 'decrypt']
                    ).then(function(key) {
                        db.transaction('k', 'readwrite').objectStore('k').put(key, 0);
                        callback(key);
                    }).catch(function(e) {
                        console.error('[EncryptedLocalStorage] generateKey failed:', e);
                        callback({_idbFailed: true});
                    });
                };
                getReq.onerror = function() {
                    callback({_idbFailed: true});
                };
            };
            """)
    private static native void openOrCreateKey(JsObjectConsumer callback);

    // -----------------------------------------------------------------------
    // AES-GCM encrypt / decrypt
    // -----------------------------------------------------------------------

    /**
     * Encrypts {@code text} with the given CryptoKey and calls back with
     * {@code base64(iv[12] || ciphertext)}, or empty string on failure.
     */
    @JSBody(params = {"key", "text", "callback"}, script = """
            if (key._idbFailed) { callback(''); return; }
            var iv = crypto.getRandomValues(new Uint8Array(12));
            var encoded = new TextEncoder().encode(text);
            crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, encoded)
            .then(function(ct) {
                var combined = new Uint8Array(12 + ct.byteLength);
                combined.set(iv, 0);
                combined.set(new Uint8Array(ct), 12);
                var binary = '';
                for (var i = 0; i < combined.length; i++) {
                    binary += String.fromCodePoint(combined[i]);
                }
                callback(btoa(binary));
            }).catch(function(e) {
                console.error('[EncryptedLocalStorage] encrypt failed:', e);
                callback('');
            });
            """)
    private static native void encryptValue(JSObject key, String text, JsStringConsumer callback);

    /**
     * Decrypts a {@code base64(iv[12] || ciphertext)} value and calls back with
     * the plaintext, or empty string on failure.
     */
    @JSBody(params = {"key", "b64", "callback"}, script = """
            if (key._idbFailed) { callback(''); return; }
            try {
                var binary = atob(b64);
                if (binary.length <= 12) { callback(''); return; }
                var combined = new Uint8Array(binary.length);
                for (var i = 0; i < binary.length; i++) combined[i] = binary.charCodeAt(i);
                var iv = combined.slice(0, 12);
                var ct = combined.slice(12);
                crypto.subtle.decrypt({ name: 'AES-GCM', iv: iv }, key, ct)
                .then(function(pt) {
                    callback(new TextDecoder().decode(new Uint8Array(pt)));
                }).catch(function(e) {
                    console.warn('[EncryptedLocalStorage] decrypt failed (stale key?):', e);
                    callback('');
                });
            } catch(e) {
                callback('');
            }
            """)
    private static native void decryptValue(JSObject key, String b64, JsStringConsumer callback);

    // -----------------------------------------------------------------------
    // localStorage JS bindings
    // -----------------------------------------------------------------------

    @JSBody(params = {"key"}, script = "try { return localStorage.getItem(key); } catch(e) { return null; }")
    private static native String lsGetItem(String key);

    @JSBody(params = {"key", "val"}, script = "try { localStorage.setItem(key, val); } catch(e) {}")
    private static native void lsSetItem(String key, String val);

    @JSBody(params = {"key"}, script = "try { localStorage.removeItem(key); } catch(e) {}")
    private static native void lsRemoveItem(String key);

    @JSBody(params = {}, script = "try { return localStorage.length; } catch(e) { return 0; }")
    private static native int lsLength();

    @JSBody(params = {"i"}, script = "try { return localStorage.key(i); } catch(e) { return null; }")
    private static native String lsKey(int i);
}
