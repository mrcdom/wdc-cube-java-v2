package br.com.wdc.framework.commons.storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória de {@link ClientStorage}.
 * <p>
 * Usada como fallback em plataformas que não suportam persistência no cliente
 * (ex: testes, server-side). Os dados existem apenas enquanto a JVM estiver ativa.
 */
public class InMemoryClientStorage implements ClientStorage {

	private final ConcurrentHashMap<String, String> map = new ConcurrentHashMap<>();

	@Override
	public ClientStorage secure() {
		return this; // dados efêmeros — backing seguro não acrescenta valor
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
