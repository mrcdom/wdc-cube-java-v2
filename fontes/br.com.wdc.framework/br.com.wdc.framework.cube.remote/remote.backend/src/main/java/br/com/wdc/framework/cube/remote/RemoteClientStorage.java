package br.com.wdc.framework.cube.remote;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import br.com.wdc.framework.commons.storage.ClientStorage;

/**
 * Implementação server-side de {@link ClientStorage} para sessões remotas (WS).
 * <p>
 * Mantém dois escopos em memória — SESSION e PERSISTENT — populados a partir
 * dos dados enviados pelo cliente no bootstrap. Quando a camada de apresentação
 * escreve um valor, ele é registrado como delta pendente e incluído no próximo
 * envelope de resposta WS, para que o cliente persista localmente.
 * <p>
 * O protocolo usa o campo {@code "storage"} com dois sub-objetos:
 * <pre>
 * {
 *   "storage": {
 *     "session":           { "key": "b64cipher(value)" },
 *     "persistent":        { "key": "b64cipher(value)" },
 *     "persistent-secure": { "key": "b64cipher(value)" }
 *   }
 * }
 * </pre>
 * Todos os valores trafegam cifrados via {@link RemoteDataSecurity#b64Cipher}.
 * O escopo {@code "persistent-secure"} instrui o cliente a usar backing seguro
 * (Keychain, FlutterSecureStorage, etc.) para armazenar o valor em repouso.
 */
public class RemoteClientStorage {

    // :: Scope constants (protocol field names)

    public static final String SCOPE_SESSION           = "session";
    public static final String SCOPE_PERSISTENT        = "persistent";
    public static final String SCOPE_PERSISTENT_SECURE = "persistent-secure";

    // :: State

    private final Map<String, String> sessionData    = new ConcurrentHashMap<>();
    private final Map<String, String> persistentData = new ConcurrentHashMap<>();

    /** null value in delta map = signal to remove the key on the client side. */
    private final Map<String, String> sessionDeltas           = new ConcurrentHashMap<>();
    private final Map<String, String> persistentDeltas        = new ConcurrentHashMap<>();
    private final Map<String, String> persistentSecureDeltas  = new ConcurrentHashMap<>();

    private RemoteDataSecurity security;

    // :: Initialization

    public void init(RemoteDataSecurity security) {
        this.security = security;
    }

    /**
     * Populates both scopes from the {@code "storage"} field of the bootstrap
     * request. All values are expected to be ciphered with the session key.
     */
    @SuppressWarnings("unchecked")
    public void loadFromBootstrap(Object rawStorage) {
        if (!(rawStorage instanceof Map<?, ?> storageMap)) {
            return;
        }
        loadScope((Map<String, Object>) storageMap.get(SCOPE_SESSION),           sessionData);
        loadScope((Map<String, Object>) storageMap.get(SCOPE_PERSISTENT),        persistentData);
        loadScope((Map<String, Object>) storageMap.get(SCOPE_PERSISTENT_SECURE), persistentData);
    }

    private void loadScope(Map<String, Object> scope, Map<String, String> target) {
        if (scope == null) {
            return;
        }
        for (var entry : scope.entrySet()) {
            var ciphered = entry.getValue();
            if (ciphered instanceof String s && !s.isBlank()) {
                try {
                    target.put(entry.getKey(), security.b64Decipher(s));
                } catch (Exception e) {
                    // skip malformed / tampered values
                }
            }
        }
    }

    /**
     * Drains all pending deltas into a map ready for JSON serialization.
     * Values are ciphered. Returns {@code null} if there are no pending deltas.
     */
    public Map<String, Map<String, String>> drainDeltas() {
        if (sessionDeltas.isEmpty() && persistentDeltas.isEmpty() && persistentSecureDeltas.isEmpty()) {
            return null;
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        drainInto(sessionDeltas,          result, SCOPE_SESSION);
        drainInto(persistentDeltas,       result, SCOPE_PERSISTENT);
        drainInto(persistentSecureDeltas, result, SCOPE_PERSISTENT_SECURE);
        return result;
    }

    private void drainInto(Map<String, String> deltas, Map<String, Map<String, String>> out, String scope) {
        if (deltas.isEmpty()) {
            return;
        }
        var scopeMap = new HashMap<String, String>();
        // putAll + clear is not atomic but acceptable: worst case a delta is
        // sent in the next response instead (no data loss, only slight delay)
        scopeMap.putAll(deltas);
        deltas.clear();
        for (var entry : scopeMap.entrySet()) {
            String wireValue = !DELTA_REMOVE.equals(entry.getValue())
                    ? security.b64Cipher(entry.getValue())
                    : null; // null = remove signal for client
            scopeMap.put(entry.getKey(), wireValue);
        }
        out.put(scope, scopeMap);
    }

    // :: ClientStorage views

    /** SESSION-scoped view (plain backing). */
    public ClientStorage session() {
        return new ScopedView(sessionData, sessionDeltas, null);
    }

    /** PERSISTENT-scoped view (plain backing on the client). */
    public ClientStorage persistent() {
        var secureView = new ScopedView(persistentData, persistentSecureDeltas, null);
        return new ScopedView(persistentData, persistentDeltas, secureView);
    }

    /** PERSISTENT + SECURE-scoped view (secure backing on the client). */
    public ClientStorage persistentSecure() {
        return new ScopedView(persistentData, persistentSecureDeltas, null);
    }

    // :: Inner implementation

    /** Sentinel stored in delta maps to signal client-side key removal (ConcurrentHashMap forbids null values). */
    private static final String DELTA_REMOVE = "\0";

    private static final class ScopedView implements ClientStorage {

        private final Map<String, String> data;
        private final Map<String, String> deltas;
        private final ClientStorage secureVariant; // null → secure() retorna this

        ScopedView(Map<String, String> data, Map<String, String> deltas, ClientStorage secureVariant) {
            this.data          = data;
            this.deltas        = deltas;
            this.secureVariant = secureVariant;
        }

        @Override
        public ClientStorage secure() {
            return secureVariant != null ? secureVariant : this;
        }

        @Override
        public String get(String key) {
            return data.get(key);
        }

        @Override
        public void set(String key, String value) {
            if (value != null) {
                data.put(key, value);
                deltas.put(key, value);
            } else {
                data.remove(key);
                deltas.put(key, DELTA_REMOVE);
            }
        }

        @Override
        public void remove(String key) {
            data.remove(key);
            deltas.put(key, DELTA_REMOVE); // sentinel = remove signal for client
        }
    }
}
