package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

import java.util.HashMap;
import java.util.Map;

import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;

/**
 * Client storage for the bridge protocol — three scopes mirroring the server
 * ({@code RemoteClientStorage}) and the other shells (TeaVM/Flutter):
 * <ul>
 * <li>{@code session} — in memory (lives while the app is open);</li>
 * <li>{@code persistent} — persists across runs (CN1 {@link Preferences});</li>
 * <li>{@code persistent-secure} — same, under a separate key ("secure" backing on the device).</li>
 * </ul>
 *
 * <p>
 * Values are kept in <b>plaintext at rest</b> and enciphered only on the wire: {@link #bootstrap}
 * enciphers everything for the 1st WS message; received deltas are deciphered and stored via {@link #apply}.
 * Each session uses a new AES key, which is why the ciphertext cannot be stored.
 * </p>
 */
public final class Cn1ClientStorage {

    public static final String SESSION = "session";
    public static final String PERSISTENT = "persistent";
    public static final String PERSISTENT_SECURE = "persistent-secure";

    private static final String PREF_PREFIX = "wdc.storage.";

    private final Map<String, String> sessionMap = new HashMap<>();
    private final Map<String, String> persistentMap;
    private final Map<String, String> persistentSecureMap;

    public Cn1ClientStorage() {
        persistentMap = load(PERSISTENT);
        persistentSecureMap = load(PERSISTENT_SECURE);
    }

    private Map<String, String> scope(String name) {
        if (PERSISTENT.equals(name)) {
            return persistentMap;
        }
        if (PERSISTENT_SECURE.equals(name)) {
            return persistentSecureMap;
        }
        return sessionMap;
    }

    /** Applies a delta from the server: {@code value==null} removes; otherwise stores the plaintext. */
    public void apply(String scopeName, String key, String value) {
        Map<String, String> m = scope(scopeName);
        if (value == null) {
            m.remove(key);
        } else {
            m.put(key, value);
        }
        persist(scopeName);
    }

    /** Bootstrap payload: each non-empty scope, with the values enciphered by the session. */
    public Map<String, Object> bootstrap(Cn1Crypto crypto) {
        Map<String, Object> out = new HashMap<>();
        putScope(out, SESSION, sessionMap, crypto);
        putScope(out, PERSISTENT, persistentMap, crypto);
        putScope(out, PERSISTENT_SECURE, persistentSecureMap, crypto);
        return out;
    }

    private void putScope(Map<String, Object> out, String name, Map<String, String> m, Cn1Crypto crypto) {
        if (m.isEmpty()) {
            return;
        }
        Map<String, Object> sub = new HashMap<>();
        for (Map.Entry<String, String> e : m.entrySet()) {
            sub.put(e.getKey(), crypto.encipher(e.getValue()));
        }
        out.put(name, sub);
    }

    // -- persistence (Preferences as a JSON blob; session is memory only) --

    private Map<String, String> load(String scopeName) {
        Map<String, String> m = new HashMap<>();
        String json = Preferences.get(PREF_PREFIX + scopeName, "");
        if (json != null && !json.isEmpty()) {
            try {
                Map<String, Object> parsed = JSONParser.parseJSON(json);
                for (Map.Entry<String, Object> e : parsed.entrySet()) {
                    if (e.getValue() != null) {
                        m.put(e.getKey(), e.getValue().toString());
                    }
                }
            } catch (Exception ignore) {
                // corrupted blob — starts empty
            }
        }
        return m;
    }

    private void persist(String scopeName) {
        if (SESSION.equals(scopeName)) {
            return; // memory only

        }
        Map<String, Object> blob = new HashMap<>(scope(scopeName));
        Preferences.set(PREF_PREFIX + scopeName, JSONParser.mapToJson(blob));
    }
}
