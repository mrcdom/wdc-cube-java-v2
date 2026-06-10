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
    // In-memory fallback (use only outside the browser)
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
    // sessionStorage-backed (session scope — survives F5, not tab close)
    // ---------------------------------------------------------------------------

    class SessionStorageClientStorage implements ClientStorage {

        private final String[] skipPrefixes;

        /**
         * @param skipPrefixes raw keys/prefixes to exclude from {@link #all()}
         */
        public SessionStorageClientStorage(String... skipPrefixes) {
            this.skipPrefixes = skipPrefixes;
        }

        @Override public ClientStorage secure() { return this; }

        @Override
        public String get(String key) { return ssGetItem(key); }

        @Override
        public void set(String key, String value) { ssSetItem(key, value); }

        @Override
        public void remove(String key) { ssRemoveItem(key); }

        @Override
        public Map<String, String> all() {
            var result = new LinkedHashMap<String, String>();
            int len = ssLength();
            outer:
            for (int i = 0; i < len; i++) {
                String key = ssKey(i);
                if (key == null) continue;
                for (String skip : skipPrefixes) {
                    if (key.startsWith(skip)) continue outer;
                }
                String v = ssGetItem(key);
                if (v != null) result.put(key, v);
            }
            return result;
        }

        @JSBody(params = {"key"}, script = "try { return sessionStorage.getItem(key); } catch(e) { return null; }")
        private static native String ssGetItem(String key);

        @JSBody(params = {"key", "val"}, script = "try { sessionStorage.setItem(key, val); } catch(e) {}")
        private static native void ssSetItem(String key, String val);

        @JSBody(params = {"key"}, script = "try { sessionStorage.removeItem(key); } catch(e) {}")
        private static native void ssRemoveItem(String key);

        @JSBody(params = {}, script = "try { return sessionStorage.length; } catch(e) { return 0; }")
        private static native int ssLength();

        @JSBody(params = {"i"}, script = "try { return sessionStorage.key(i); } catch(e) { return null; }")
        private static native String ssKey(int i);
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
         * @param shellId       short identifier for the shell (e.g. {@code "rt"}) used as
         *                      a localStorage namespace prefix — keeps each shell's data isolated
         *                      while sharing the same IndexedDB AES key
         * @param skipPrefixes  raw key prefixes to exclude from {@link #all()}
         * @param secureFactory factory that produces the {@link #secure()} view
         */
        public LocalStorageClientStorage(String shellId, String[] skipPrefixes,
                Supplier<ClientStorage> secureFactory) {
            this.keyPrefix = shellId.isEmpty() ? "" : shellId + ":";
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
                // Only sync keys prefixed with '~'
                if (!shortKey.startsWith("~")) continue;
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
