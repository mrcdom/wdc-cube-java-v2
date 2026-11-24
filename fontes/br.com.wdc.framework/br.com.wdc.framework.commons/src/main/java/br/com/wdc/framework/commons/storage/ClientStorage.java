package br.com.wdc.framework.commons.storage;


/**
 * Client-side key-value storage abstraction.
 * <p>
 * Each platform/context provides two implementations, one per scope:
 * <ul>
 *   <li>{@code app.clientSessionStore()} — in-memory, lives while the app instance
 *       (or the connection, in the case of {@code HostClient}) is active.</li>
 *   <li>{@code app.clientPersistentStore()} — persists across restarts.
 *       On desktop/mobile it uses {@link PreferencesClientStorage};
 *       in the probe/bridge it uses {@link InMemoryClientStorage} by default.</li>
 * </ul>
 * To store sensitive values, use {@link #secure()} on either scope:
 * <pre>
 *   app.clientPersistentStore().secure().set("auth.token", token);
 *   app.clientSessionStore().secure().set("csrf.token", csrf);
 * </pre>
 * <p>
 * The behavior of {@link #secure()} depends on the implementation:
 * <ul>
 *   <li>{@link PreferencesClientStorage} — returns {@link EncryptedPreferencesClientStorage}
 *       (AES-256-GCM, key generated per installation).</li>
 *   <li>{@link InMemoryClientStorage} — returns {@code this}; ephemeral data, encryption
 *       adds no value.</li>
 * </ul>
 */
public interface ClientStorage {

    /**
     * Returns a view of this scope with a secure backing for values at rest.
     * <p>
     * {@link PreferencesClientStorage} returns {@link EncryptedPreferencesClientStorage}
     * (AES-256-GCM). {@link InMemoryClientStorage} returns {@code this} — the data
     * is ephemeral and encryption adds no value in this context.
     */
    ClientStorage secure();

    /**
     * Returns the value associated with the key, or {@code null} if there is none.
     */
    String get(String key);

    /**
     * Stores a value associated with the key.
     */
    void set(String key, String value);

    /**
     * Removes the entry associated with the key.
     */
    void remove(String key);

    /**
     * Returns every entry of this scope that is a candidate for synchronization.
     * By convention, only keys prefixed with {@code ~} are included.
     * <p>
     * Implementations that do not support synchronization (e.g. in-memory, desktop)
     * may return the default empty map.
     */
    default java.util.Map<String, String> all() {
        return java.util.Collections.emptyMap();
    }

}
