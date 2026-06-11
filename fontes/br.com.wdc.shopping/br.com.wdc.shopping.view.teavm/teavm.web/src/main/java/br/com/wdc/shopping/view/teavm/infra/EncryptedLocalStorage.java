package br.com.wdc.shopping.view.teavm.infra;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.shopping.view.teavm.commons.interop.JsObjectConsumer;
import br.com.wdc.shopping.view.teavm.commons.interop.JsStringConsumer;

/**
 * Implementação de {@link ClientStorage} com criptografia AES-GCM via Web Crypto API.
 * <p>
 * A chave AES-256 é criada com {@code extractable: false} e persiste no IndexedDB
 * da origem. Por ser não-exportável, os bytes da chave nunca ficam acessíveis ao
 * JavaScript — nem por XSS. Os valores são cifrados com IV aleatório por entrada
 * e armazenados em {@code localStorage} no formato {@code base64(iv[12] + ciphertext)}.
 * <p>
 * Fluxo de inicialização:
 * <ol>
 *   <li>Abre (ou cria) IndexedDB {@code _sec}, object store {@code k}.</li>
 *   <li>Se a chave já existe (reloads subsequentes): recupera e usa.</li>
 *   <li>Se não existe (primeira execução): gera com
 *       {@code crypto.subtle.generateKey}, armazena no IndexedDB.</li>
 *   <li>Decifra todas as entradas {@code sec.*} do localStorage para o cache em memória.</li>
 *   <li>Chama {@code onReady} — a partir daí, leituras e escritas são síncronas
 *       (via cache) com persistência assíncrona.</li>
 * </ol>
 * <p>
 * Fallback: se o IndexedDB não estiver disponível (ex.: modo privado em alguns browsers),
 * o storage funciona somente em memória (valores não sobrevivem ao reload).
 * <p>
 * <strong>Deve-se chamar {@link #initialize(Runnable)} antes de usar o storage.</strong>
 */
public class EncryptedLocalStorage implements ClientStorage {

    public static final EncryptedLocalStorage INSTANCE = new EncryptedLocalStorage();

    /** Cache em memória dos valores decifrados. Populado durante {@link #initialize}. */
    private final Map<String, String> cache = new LinkedHashMap<>();

    /** Chave AES-GCM não-exportável, obtida do IndexedDB. Null se indisponível. */
    private static JSObject cryptoKey;

    /**
     * Prefixo no localStorage: {@code "{shellId}:sec."}.
     * Configurado por {@link #configure(String)} antes de {@link #initialize(Runnable)}.
     */
    private static String LS_PREFIX = "sec.";

    /**
     * Configura o prefixo de namespace específico do shell.
     * Deve ser chamado antes de {@link #initialize(Runnable)}.
     *
     * @param shellId identificador curto do shell (ex: {@code "tw"})
     */
    public static void configure(String shellId) {
        LS_PREFIX = shellId + ":sec.";
    }

    private EncryptedLocalStorage() {
    }

    /**
     * Inicializa a chave de criptografia e pré-decifra todos os valores armazenados
     * para o cache em memória. Deve ser chamado uma vez antes de {@code app.start()}.
     *
     * @param onReady callback chamado quando o storage está pronto para uso
     */
    public static void initialize(Runnable onReady) {
        openOrCreateKey(key -> {
            cryptoKey = key;
            if (key == null) {
                // IndexedDB indisponível — funciona apenas em memória
                onReady.run();
                return;
            }

            // Coleta snapshot síncrono das chaves sec.* no localStorage
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
                    // Else: valor com chave antiga/corrompido — descartado silenciosamente
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
            // Persiste assincronamente no localStorage cifrado.
            // Verifica cache.containsKey ao final para evitar race condition com remove():
            // se remove() foi chamado antes do callback completar, não grava no localStorage.
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
     * Abre (ou cria) o IndexedDB {@code _sec} e retorna a chave AES-GCM armazenada.
     * Se não existir, gera uma nova chave não-exportável e a persiste no IndexedDB.
     * Chama callback com {@code null} se o IndexedDB não estiver disponível.
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
     * Cifra {@code text} com AES-GCM usando a chave fornecida.
     * Retorna {@code base64(iv[12] + ciphertext)} via callback, ou string vazia em caso de erro.
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
     * Decifra um valor {@code base64(iv[12] + ciphertext)} produzido por {@link #encryptValue}.
     * Retorna o texto plano via callback, ou string vazia se falhar
     * (chave diferente, dado corrompido, etc.).
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
