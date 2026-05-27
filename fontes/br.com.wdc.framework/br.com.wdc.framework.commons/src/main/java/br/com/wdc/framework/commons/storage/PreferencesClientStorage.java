package br.com.wdc.framework.commons.storage;

import java.util.prefs.Preferences;

/**
 * Implementação de {@link ClientStorage} usando {@link Preferences} do JDK.
 * <p>
 * Persiste dados entre reinicializações da aplicação em plataformas JVM
 * (desktop, mobile via Gluon). O armazenamento é local ao usuário do SO.
 */
public class PreferencesClientStorage implements ClientStorage {

	private final Preferences prefs;

	/**
	 * Cria um storage usando o nó de preferências da classe fornecida.
	 *
	 * @param appClass classe que identifica o escopo do armazenamento
	 */
	public PreferencesClientStorage(Class<?> appClass) {
		this.prefs = Preferences.userNodeForPackage(appClass);
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

}
