package br.com.wdc.framework.commons.storage;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * {@link ClientStorage} implementation backed by the JDK {@link Preferences}.
 * <p>
 * Persists data across application restarts on JVM platforms
 * (desktop). The storage is local to the OS user.
 * <p>
 * {@link #secure()} returns an {@link EncryptedPreferencesClientStorage} that
 * shares the same {@link Preferences} node, storing the values
 * encrypted with AES-256-GCM. The AES key is generated once per installation and
 * reused across restarts. Encrypted entries use the prefix
 * {@value EncryptedPreferencesClientStorage#KEY_PREFIX} so they do not collide with
 * this storage's plaintext entries.
 */
public class PreferencesClientStorage implements ClientStorage {

	private final Preferences prefs;
	private final ClientStorage secureView;

	/**
	 * Creates a storage using the preferences node of the given class.
	 *
	 * @param appClass class that identifies the storage scope
	 */
	public PreferencesClientStorage(Class<?> appClass) {
		this.prefs = Preferences.userNodeForPackage(appClass);
		this.secureView = new EncryptedPreferencesClientStorage(this.prefs);
	}

	@Override
	public ClientStorage secure() {
		return secureView;
	}

	@Override
	public String get(String key) {
		return prefs.get(key, null);
	}

	@Override
	public void set(String key, String value) {
		if (value != null) {
			prefs.put(key, value);
		} else {
			prefs.remove(key);
		}
	}

	@Override
	public void remove(String key) {
		prefs.remove(key);
	}

	@Override
	public Map<String, String> all() {
		var result = new LinkedHashMap<String, String>();
		try {
			for (String key : prefs.keys()) {
				String value = prefs.get(key, null);
				if (value != null) {
					result.put(key, value);
				}
			}
		} catch (BackingStoreException e) {
			// Best-effort: return what we have
		}
		return result;
	}

}
