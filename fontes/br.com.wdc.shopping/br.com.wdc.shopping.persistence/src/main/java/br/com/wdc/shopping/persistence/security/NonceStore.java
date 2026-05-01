package br.com.wdc.shopping.persistence.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Armazena nonces descartáveis para o fluxo challenge-response de login.
 * <p>
 * Cada nonce é de uso único e expira após um TTL curto.
 */
public final class NonceStore {

	private static final Duration NONCE_TTL = Duration.ofSeconds(60);

	private final ConcurrentHashMap<String, Instant> nonces = new ConcurrentHashMap<>();

	/**
	 * Gera e armazena um novo nonce.
	 */
	public String generate() {
		var nonce = UUID.randomUUID().toString();
		nonces.put(nonce, Instant.now().plus(NONCE_TTL));
		return nonce;
	}

	/**
	 * Retorna a data de expiração do nonce, sem removê-lo.
	 */
	public Instant expiresAt(String nonce) {
		return nonces.get(nonce);
	}

	/**
	 * Consome o nonce (uso único). Retorna true se válido e não expirado.
	 */
	public boolean consume(String nonce) {
		var expiresAt = nonces.remove(nonce);
		return expiresAt != null && Instant.now().isBefore(expiresAt);
	}

	/**
	 * Remove nonces expirados. Chamado periodicamente.
	 */
	public void evictExpired() {
		var now = Instant.now();
		Iterator<Map.Entry<String, Instant>> it = nonces.entrySet().iterator();
		while (it.hasNext()) {
			if (now.isAfter(it.next().getValue())) {
				it.remove();
			}
		}
	}
}
