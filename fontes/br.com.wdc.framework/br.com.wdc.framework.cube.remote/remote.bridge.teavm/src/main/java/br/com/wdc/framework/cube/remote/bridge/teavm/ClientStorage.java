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

        private final String syncNamespace;
        private ClientStorage _secure;
        private final Supplier<ClientStorage> secureFactory;

        /**
         * @param syncNamespace namespace prefix for sync (e.g. {@code "~rt:"})
         * @param secureFactory factory for the {@link #secure()} view; pass {@code null}
         *                      to fall back to {@code this} (no encryption)
         */
        public SessionStorageClientStorage(String syncNamespace, Supplier<ClientStorage> secureFactory) {
            this.syncNamespace = syncNamespace;
            this.secureFactory = secureFactory;
        }

        @Override
        public ClientStorage secure() {
            if (secureFactory == null) return this;
            if (_secure == null) _secure = secureFactory.get();
            return _secure;
        }

        @Override
        public String get(String key) { return ssGetItem(syncNamespace + key); }

        @Override
        public void set(String key, String value) { ssSetItem(syncNamespace + key, value); }

        @Override
        public void remove(String key) { ssRemoveItem(syncNamespace + key); }

        @Override
        public Map<String, String> all() {
            var result = new LinkedHashMap<String, String>();
            int len = ssLength();
            for (int i = 0; i < len; i++) {
                String rawKey = ssKey(i);
                if (rawKey == null || !rawKey.startsWith(syncNamespace)) continue;
                String shortKey = rawKey.substring(syncNamespace.length());
                String v = ssGetItem(rawKey);
                if (v != null) result.put(shortKey, v);
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

        private final String syncNamespace;
        private final Supplier<ClientStorage> secureFactory;
        private ClientStorage _secure;

        /**
         * @param syncNamespace namespace prefix that qualifies entries for WebSocket sync
         *                      (e.g. {@code "~rt:"}); prepended to every stored key and
         *                      stripped in {@link #all()}
         * @param secureFactory factory that produces the {@link #secure()} view
         */
        public LocalStorageClientStorage(String syncNamespace, Supplier<ClientStorage> secureFactory) {
            this.syncNamespace = syncNamespace;
            this.secureFactory = secureFactory;
        }

        @Override
        public ClientStorage secure() {
            if (_secure == null) _secure = secureFactory.get();
            return _secure;
        }

        @Override
        public String get(String key) { return lsGetItem(syncNamespace + key); }

        @Override
        public void set(String key, String value) { lsSetItem(syncNamespace + key, value); }

        @Override
        public void remove(String key) { lsRemoveItem(syncNamespace + key); }

        @Override
        public Map<String, String> all() {
            var result = new LinkedHashMap<String, String>();
            int len = lsLength();
            for (int i = 0; i < len; i++) {
                String rawKey = lsKey(i);
                if (rawKey == null || !rawKey.startsWith(syncNamespace)) continue;
                String shortKey = rawKey.substring(syncNamespace.length());
                String v = lsGetItem(rawKey);
                if (v != null) result.put(shortKey, v);
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
