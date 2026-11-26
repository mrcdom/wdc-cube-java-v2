package br.com.wdc.store.persistence.impl.security;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The nonces of the sign-in challenges: each one is for one user, single-use, and expires after a short TTL.
 */
public final class NonceStore {

	private static final Duration NONCE_TTL = Duration.ofSeconds(60);

	private record Entry(String userName, Instant expiresAt) {
	}

	private final ConcurrentHashMap<String, Entry> nonces = new ConcurrentHashMap<>();

	/** Generates and stores a new nonce for {@code userName}'s sign-in. */
	public String generate(String userName) {
		var nonce = UUID.randomUUID().toString();
		nonces.put(nonce, new Entry(userName, Instant.now().plus(NONCE_TTL)));
		return nonce;
	}

	/** The nonce's expiration time, without consuming it; {@code null} for an unknown nonce. */
	public Instant expiresAt(String nonce) {
		var entry = nonces.get(nonce);
		return entry == null ? null : entry.expiresAt();
	}

	/**
	 * Consumes the nonce (single use, even when refused): {@code true} when it exists, has not expired and was made for
	 * {@code userName}.
	 */
	public boolean consume(String nonce, String userName) {
		var entry = nonce == null ? null : nonces.remove(nonce);
		return entry != null && Instant.now().isBefore(entry.expiresAt()) && Objects.equals(entry.userName(), userName);
	}

	/** Removes expired nonces. Called periodically. */
	public void evictExpired() {
		var now = Instant.now();
		nonces.values().removeIf(entry -> now.isAfter(entry.expiresAt()));
	}
}
