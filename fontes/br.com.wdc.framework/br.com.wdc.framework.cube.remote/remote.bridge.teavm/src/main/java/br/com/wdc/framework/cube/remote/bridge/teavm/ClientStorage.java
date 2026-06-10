package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.teavm.jso.JSBody;

/**
 * Client-side key-value storage abstraction.
 * <p>
 * Mirrors the Java/Dart/TypeScript {@code ClientStorage} design: two scopes
 * (session and persistent) with a {@link #secure()} view for sensitive values.
 * <p>
 * All values that travel over WebSocket are ciphered regardless of the scope.
 * The {@code .secure()} view is a hint to use a more secure backing store.
 * In the browser there is no Keychain, so secure storage uses {@code localStorage}
 * with a {@code sec.} key prefix to namespace it away from plain persistent values.
 */
public interface ClientStorage {

    /** Returns a view that uses secure backing (or {@code this} if already secure). */
    ClientStorage secure();

    String get(String key);

    void set(String key, String value);

    void remove(String key);

    /** All entries in this scope. Used when building the WebSocket bootstrap payload. */
    Map<String, String> all();

    // ---------------------------------------------------------------------------
    // In-memory implementation (session scope / fallback)
    // ---------------------------------------------------------------------------

    class InMemoryClientStorage implements ClientStorage {
        private final Map<String, String> data = new LinkedHashMap<>();

        @Override public ClientStorage secure() { return this; }
        @Override public String get(String key) { return data.get(key); }
        @Override public void set(String key, String value) { data.put(key, value); }
        @Override public void remove(String key) { data.remove(key); }
        @Override public Map<String, String> all() { return new LinkedHashMap<>(data); }
    }

    // ---------------------------------------------------------------------------
    // localStorage-backed implementation (persistent scope)
    // ---------------------------------------------------------------------------

    class LocalStorageClientStorage implements ClientStorage {

        private final String keyPrefix;
        private final String[] skipPrefixes;
        private final Supplier<ClientStorage> secureFactory;
        private ClientStorage _secure;

        /**
         * @param keyPrefix     raw key prefix in localStorage (e.g. {@code ""} for plain,
         *                      {@code "sec."} for secure-namespaced)
         * @param skipPrefixes  raw key prefixes to exclude from {@link #all()}
         * @param secureFactory factory that produces the {@link #secure()} view
         */
        public LocalStorageClientStorage(String keyPrefix, String[] skipPrefixes,
                Supplier<ClientStorage> secureFactory) {
            this.keyPrefix = keyPrefix;
            this.skipPrefixes = skipPrefixes;
            this.secureFactory = secureFactory;
        }

        @Override
        public ClientStorage secure() {
            if (_secure == null) {
                _secure = secureFactory.get();
            }
            return _secure;
        }

        @Override
        public String get(String key) {
            return lsGetItem(keyPrefix + key);
        }

        @Override
        public void set(String key, String value) {
            lsSetItem(keyPrefix + key, value);
        }

        @Override
        public void remove(String key) {
            lsRemoveItem(keyPrefix + key);
        }

        @Override
        public Map<String, String> all() {
            var result = new LinkedHashMap<String, String>();
            int len = lsLength();
            outer:
            for (int i = 0; i < len; i++) {
                String rawKey = lsKey(i);
                if (rawKey == null) continue;
                if (!rawKey.startsWith(keyPrefix)) continue;
                for (String skip : skipPrefixes) {
                    if (rawKey.startsWith(skip)) continue outer;
                }
                String shortKey = rawKey.substring(keyPrefix.length());
                String v = lsGetItem(rawKey);
                if (v != null) {
                    result.put(shortKey, v);
                }
            }
            return result;
        }

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
}
