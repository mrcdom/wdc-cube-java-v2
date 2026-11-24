package br.com.wdc.store.persistence.impl.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores disposable nonces for the challenge-response login flow.
 * <p>
 * Each nonce is single-use and expires after a short TTL.
 */
public final class NonceStore {

	private static final Duration NONCE_TTL = Duration.ofSeconds(60);

	private final ConcurrentHashMap<String, Instant> nonces = new ConcurrentHashMap<>();

	/**
	 * Generates and stores a new nonce.
	 */
	public String generate() {
		var nonce = UUID.randomUUID().toString();
		nonces.put(nonce, Instant.now().plus(NONCE_TTL));
		return nonce;
	}

	/**
	 * Returns the nonce's expiration time without removing it.
	 */
	public Instant expiresAt(String nonce) {
		return nonces.get(nonce);
	}

	/**
	 * Consumes the nonce (single use). Returns true if valid and not expired.
	 */
	public boolean consume(String nonce) {
		var expiresAt = nonces.remove(nonce);
		return expiresAt != null && Instant.now().isBefore(expiresAt);
	}

	/**
	 * Removes expired nonces. Called periodically.
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
