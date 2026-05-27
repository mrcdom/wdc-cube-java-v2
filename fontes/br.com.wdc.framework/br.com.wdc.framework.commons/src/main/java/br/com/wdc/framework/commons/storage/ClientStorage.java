package br.com.wdc.framework.commons.storage;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Abstração de armazenamento chave-valor no cliente.
 * <p>
 * Cada plataforma fornece sua implementação:
 * <ul>
 *   <li>Browser (TeaVM): {@code window.sessionStorage}</li>
 *   <li>Desktop/Mobile (Gluon/Swing): Java Preferences ou file-based</li>
 * </ul>
 */
public interface ClientStorage {

	AtomicReference<ClientStorage> BEAN = new AtomicReference<>();

	/**
	 * Obtém o valor associado à chave, ou {@code null} se não existir.
	 */
	String get(String key);

	/**
	 * Armazena um valor associado à chave.
	 */
	void set(String key, String value);

	/**
	 * Remove a entrada associada à chave.
	 */
	void remove(String key);

}
