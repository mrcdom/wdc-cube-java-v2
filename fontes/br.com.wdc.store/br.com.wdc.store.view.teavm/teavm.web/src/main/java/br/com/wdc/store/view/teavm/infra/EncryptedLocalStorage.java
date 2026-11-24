package br.com.wdc.store.view.teavm.infra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.store.view.teavm.commons.interop.JsObjectConsumer;
import br.com.wdc.store.view.teavm.commons.interop.JsStringConsumer;

/**
 * {@link ClientStorage} implementation with AES-GCM encryption through the Web Crypto API.
 * <p>
 * The AES-256 key is created with {@code extractable: false} and persists in the origin's
 * IndexedDB. Being non-exportable, the key bytes are never accessible to
 * JavaScript — not even through XSS. Values are encrypted with a random IV per entry
 * and stored in {@code localStorage} in the {@code base64(iv[12] + ciphertext)} format.
 * <p>
 * Initialization flow:
 * <ol>
 *   <li>Opens (or creates) IndexedDB {@code _sec}, object store {@code k}.</li>
 *   <li>If the key already exists (subsequent reloads): retrieves and uses it.</li>
 *   <li>If it does not exist (first run): generates it with
 *       {@code crypto.subtle.generateKey} and stores it in IndexedDB.</li>
 *   <li>Decrypts all {@code sec.*} entries of localStorage into the in-memory cache.</li>
 *   <li>Calls {@code onReady} — from then on, reads and writes are synchronous
 *       (through the cache) with asynchronous persistence.</li>
 * </ol>
 * <p>
 * Fallback: if IndexedDB is not available (e.g. private mode in some browsers),
 * the storage works in memory only (values do not survive a reload).
 * <p>
 * <strong>{@link #initialize(Runnable)} must be called before using the storage.</strong>
 */
public class EncryptedLocalStorage implements ClientStorage {

    public static final EncryptedLocalStorage INSTANCE = new EncryptedLocalStorage();

    /** In-memory cache of the decrypted values. Populated during {@link #initialize}. */
    private final Map<String, String> cache = new LinkedHashMap<>();

    /** Non-exportable AES-GCM key, obtained from IndexedDB. Null if unavailable. */
    private static JSObject cryptoKey;

    /**
     * Prefix in localStorage: {@code "{shellId}:sec."}.
     * Set by {@link #configure(String)} before {@link #initialize(Runnable)}.
     */
    private static String LS_PREFIX = "sec.";

    /**
     * Sets the shell-specific namespace prefix.
     * Must be called before {@link #initialize(Runnable)}.
     *
     * @param shellId short shell identifier (e.g. {@code "tw"})
     */
    public static void configure(String shellId) {
        LS_PREFIX = shellId + ":sec.";
    }

    private EncryptedLocalStorage() {
    }

    /**
     * Initializes the encryption key and pre-decrypts all stored values
     * into the in-memory cache. Must be called once before {@code app.start()}.
     *
     * @param onReady callback called when the storage is ready for use
     */
    public static void initialize(Runnable onReady) {
        openOrCreateKey(key -> {
            cryptoKey = key;
            if (key == null) {
                // IndexedDB unavailable — works in memory only
                onReady.run();
                return;
            }

            // Takes a synchronous snapshot of the sec.* keys in localStorage
            int len = lsLength();
            var secEntries = new ArrayList<String>();
            for (int i = 0; i < len; i++) {
                String rawKey = lsKey(i);
                if (rawKey != null && rawKey.startsWith(LS_PREFIX)) {
                    secEntries.add(rawKey);
                }
            }

            if (secEntries.isEmpty()) {
                onReady.run();
                return;
            }

            int[] remaining = { secEntries.size() };

            for (String rawKey : secEntries) {
                String shortKey = rawKey.substring(LS_PREFIX.length());
                String b64 = lsGetItem(rawKey);
                if (b64 == null || b64.isEmpty()) {
                    if (--remaining[0] == 0) onReady.run();
                    continue;
                }
                decryptValue(key, b64, plaintext -> {
                    if (plaintext != null && !plaintext.isEmpty()) {
                        INSTANCE.cache.put(shortKey, plaintext);
                    }
                    // Else: value with an old/corrupted key — silently discarded
                    if (--remaining[0] == 0) onReady.run();
                });
            }
        });
    }

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
        if (value == null) {
            remove(key);
            return;
        }
        cache.put(key, value);
        if (cryptoKey != null) {
            // Persists asynchronously to the encrypted localStorage.
            // Checks cache.containsKey at the end to avoid a race condition with remove():
            // if remove() was called before the callback completed, it does not write to localStorage.
            encryptValue(cryptoKey, value, b64 -> {
                if (b64 != null && !b64.isEmpty() && cache.containsKey(key)) {
                    lsSetItem(LS_PREFIX + key, b64);
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

    // -- JS interop --

    /**
     * Opens (or creates) the IndexedDB {@code _sec} and returns the stored AES-GCM key.
     * If it does not exist, generates a new non-exportable key and persists it in IndexedDB.
     * Calls callback with {@code null} if IndexedDB is not available.
     */
    @JSBody(params = { "callback" }, script = """
            try {
                var req = indexedDB.open('_sec', 1);
                req.onupgradeneeded = function(e) {
                    e.target.result.createObjectStore('k');
                };
                req.onsuccess = function(e) {
                    var db = e.target.result;
                    var tx = db.transaction('k', 'readwrite');
                    var store = tx.objectStore('k');
                    var get = store.get(0);
                    get.onsuccess = function() {
                        if (get.result) {
                            callback(get.result);
                        } else {
                            crypto.subtle.generateKey(
                                { name: 'AES-GCM', length: 256 },
                                false,
                                ['encrypt', 'decrypt']
                            ).then(function(key) {
                                var tx2 = db.transaction('k', 'readwrite');
                                tx2.objectStore('k').put(key, 0);
                                callback(key);
                            }).catch(function(err) {
                                console.error('[EncryptedStorage] key generation failed:', err);
                                callback(null);
                            });
                        }
                    };
                    get.onerror = function() { callback(null); };
                };
                req.onerror = function(e) {
                    console.warn('[EncryptedStorage] IndexedDB open failed:', e.target.error);
                    callback(null);
                };
            } catch(e) {
                console.warn('[EncryptedStorage] IndexedDB unavailable:', e);
                callback(null);
            }
            """)
    private static native void openOrCreateKey(JsObjectConsumer callback);

    /**
     * Encrypts {@code text} with AES-GCM using the given key.
     * Returns {@code base64(iv[12] + ciphertext)} through callback, or an empty string on error.
     */
    @JSBody(params = { "key", "text", "callback" }, script = """
            var iv = crypto.getRandomValues(new Uint8Array(12));
            crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key,
                    new TextEncoder().encode(text))
            .then(function(ct) {
                var b = new Uint8Array(12 + ct.byteLength);
                b.set(iv, 0);
                b.set(new Uint8Array(ct), 12);
                var s = '';
                for (var i = 0; i < b.length; i++) s += String.fromCharCode(b[i]);
                callback(btoa(s));
            }).catch(function(err) {
                console.error('[EncryptedStorage] encrypt failed:', err);
                callback('');
            });
            """)
    private static native void encryptValue(JSObject key, String text, JsStringConsumer callback);

    /**
     * Decrypts a {@code base64(iv[12] + ciphertext)} value produced by {@link #encryptValue}.
     * Returns the plain text through callback, or an empty string if it fails
     * (different key, corrupted data, etc.).
     */
    @JSBody(params = { "key", "b64", "callback" }, script = """
            try {
                var s = atob(b64);
                var b = new Uint8Array(s.length);
                for (var i = 0; i < s.length; i++) b[i] = s.charCodeAt(i);
                crypto.subtle.decrypt({ name: 'AES-GCM', iv: b.slice(0, 12) },
                        key, b.slice(12))
                .then(function(pt) {
                    callback(new TextDecoder().decode(new Uint8Array(pt)));
                }).catch(function(err) {
                    console.warn('[EncryptedStorage] decrypt failed (stale key or corrupted data):', err);
                    callback('');
                });
            } catch(e) {
                callback('');
            }
            """)
    private static native void decryptValue(JSObject key, String b64, JsStringConsumer callback);

    @JSBody(params = {}, script = "try { return localStorage.length; } catch(e) { return 0; }")
    private static native int lsLength();

    @JSBody(params = { "i" }, script = "try { return localStorage.key(i); } catch(e) { return null; }")
    private static native String lsKey(int i);

    @JSBody(params = { "key" }, script = "try { return localStorage.getItem(key); } catch(e) { return null; }")
    private static native String lsGetItem(String key);

    @JSBody(params = { "key", "val" }, script = "try { localStorage.setItem(key, val); } catch(e) {}")
    private static native void lsSetItem(String key, String val);

    @JSBody(params = { "key" }, script = "try { localStorage.removeItem(key); } catch(e) {}")
    private static native void lsRemoveItem(String key);
}
