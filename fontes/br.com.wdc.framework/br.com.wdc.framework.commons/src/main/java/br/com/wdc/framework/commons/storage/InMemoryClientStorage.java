package br.com.wdc.framework.commons.storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ClientStorage}.
 * <p>
 * Used in the following contexts:
 * <ul>
 *   <li>{@code clientSessionStore()} on every platform — data of the current session.</li>
 *   <li>{@code clientPersistentStore()} in the probe/bridge ({@code HostClient}) — no real
 *       persistence; data lives while the connection is active.</li>
 *   <li>Tests and server-side environments where local persistence is not wanted.</li>
 * </ul>
 * {@link #secure()} returns {@code this} — the data is already ephemeral and encryption
 * adds no value in this context.
 */
public class InMemoryClientStorage implements ClientStorage {

	private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

	@Override
	public ClientStorage secure() {
		return this; // ephemeral data — a secure backing adds no value
	}

	@Override
	public String get(String key) {
		return map.get(key);
	}

	@Override
	public void set(String key, String value) {
		if (value != null) {
			map.put(key, value);
		} else {
			map.remove(key);
		}
	}

	@Override
	public void remove(String key) {
		map.remove(key);
	}

}
