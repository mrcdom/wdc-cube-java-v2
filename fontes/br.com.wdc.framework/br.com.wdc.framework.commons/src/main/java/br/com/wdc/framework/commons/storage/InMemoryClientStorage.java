package br.com.wdc.framework.commons.storage;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementação em memória de {@link ClientStorage}.
 * <p>
 * Usada nos seguintes contextos:
 * <ul>
 *   <li>{@code clientSessionStore()} em todas as plataformas — dados da sessão corrente.</li>
 *   <li>{@code clientPersistentStore()} no probe/bridge ({@code HostClient}) — sem persistência
 *       real; dados vivem enquanto a conexão estiver ativa.</li>
 *   <li>Testes e ambientes server-side onde persistência local não é desejada.</li>
 * </ul>
 * {@link #secure()} retorna {@code this} — os dados já são efêmeros e criptografia
 * não agrega valor nesse contexto.
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
