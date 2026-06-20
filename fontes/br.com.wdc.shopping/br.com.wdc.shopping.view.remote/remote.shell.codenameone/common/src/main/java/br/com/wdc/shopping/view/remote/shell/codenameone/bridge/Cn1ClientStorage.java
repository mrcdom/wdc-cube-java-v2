package br.com.wdc.shopping.view.remote.shell.codenameone.bridge;

import java.util.HashMap;
import java.util.Map;

import com.codename1.io.JSONParser;
import com.codename1.io.Preferences;

/**
 * Storage do cliente para o protocolo bridge — três escopos espelhando o servidor
 * ({@code RemoteClientStorage}) e os demais shells (TeaVM/Flutter):
 * <ul>
 * <li>{@code session} — em memória (vive enquanto o app está aberto);</li>
 * <li>{@code persistent} — persiste entre execuções (CN1 {@link Preferences});</li>
 * <li>{@code persistent-secure} — idem, em chave separada (backing "seguro" no dispositivo).</li>
 * </ul>
 *
 * <p>
 * Os valores ficam em <b>plaintext em repouso</b> e são cifrados só no fio: o {@link #bootstrap}
 * cifra tudo para a 1ª mensagem WS; os deltas recebidos são decifrados e gravados via {@link #apply}.
 * Cada sessão usa uma chave AES nova, por isso não se pode guardar o ciphertext.
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

    /** Aplica um delta vindo do servidor: {@code value==null} remove; senão grava o plaintext. */
    public void apply(String scopeName, String key, String value) {
        Map<String, String> m = scope(scopeName);
        if (value == null) {
            m.remove(key);
        } else {
            m.put(key, value);
        }
        persist(scopeName);
    }

    /** Payload do bootstrap: cada escopo não-vazio, com os valores cifrados pela sessão. */
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

    // -- persistência (Preferences como blob JSON; session é só memória) --

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
                // blob corrompido — começa vazio
            }
        }
        return m;
    }

    private void persist(String scopeName) {
        if (SESSION.equals(scopeName)) {
            return; // só memória
        }
        Map<String, Object> blob = new HashMap<>(scope(scopeName));
        Preferences.set(PREF_PREFIX + scopeName, JSONParser.mapToJson(blob));
    }
}
