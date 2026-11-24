package br.com.wdc.store.persistence.impl.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import br.com.wdc.framework.commons.log.Log;

/**
 * In-memory cache of authenticated sessions.
 * <p>
 * Supports multiple active sessions per user (multi-device).
 * Indexed by refreshToken — each device/tab keeps its own session.
 */
public final class AccessContextCache {

	private static final Log LOG = Log.getLogger(AccessContextCache.class);

	private static final int RSA_KEY_SIZE = 2048;
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

	/** Primary index: refreshToken → AccessContext. */
	private final ConcurrentHashMap<String, AccessContext> byRefreshToken = new ConcurrentHashMap<>();

	private final String jwtSecret;

	public AccessContextCache(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	/**
	 * Creates a new session for the authenticated user.
	 * Earlier sessions of the same user are <em>not</em> removed —
	 * each device has its own independent session.
	 */
	public AccessContext createSession(Long userId, String userName, Set<String> permissions) {
		var keyPair = generateRsaKeyPair();
		var refreshToken = UUID.randomUUID().toString();
		var expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);

		var ctx = new AccessContext(userId, userName, permissions, keyPair, expiresAt, refreshToken);
		byRefreshToken.put(refreshToken, ctx);

		LOG.info("Session created for user: {} ({})", userName, userId);
		return ctx;
	}

	/**
	 * Renews the access token using the refresh token.
	 * Returns null if the refresh token is invalid or expired.
	 */
	public AccessContext refresh(String refreshToken) {
		var existing = byRefreshToken.remove(refreshToken);
		if (existing == null) {
			return null;
		}

		var newRefreshToken = UUID.randomUUID().toString();
		var expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);

		var ctx = new AccessContext(existing.userId(), existing.userName(), existing.permissions(),
				generateRsaKeyPair(), expiresAt, newRefreshToken);
		byRefreshToken.put(newRefreshToken, ctx);

		LOG.info("Session refreshed for user: {} ({})", existing.userName(), existing.userId());
		return ctx;
	}

	/**
	 * Finds any valid (unexpired) session for the userId.
	 * Used after JWT validation to obtain the permissions and RSA key.
	 */
	public AccessContext get(Long userId) {
		for (var ctx : byRefreshToken.values()) {
			if (userId.equals(ctx.userId()) && !ctx.isExpired()) {
				return ctx;
			}
		}
		return null;
	}

	/**
	 * Removes (logout) <em>all</em> of the user's sessions.
	 */
	public void remove(Long userId) {
		byRefreshToken.entrySet().removeIf(e -> userId.equals(e.getValue().userId()));
		LOG.info("All sessions removed for userId: {}", userId);
	}

	/**
	 * Removes only the session tied to the refresh token (logout of one device).
	 *
	 * @return true if the session was found and removed
	 */
	public boolean removeByRefreshToken(String refreshToken) {
		return byRefreshToken.remove(refreshToken) != null;
	}

	/**
	 * Removes sessions whose refresh token has expired. Called periodically.
	 */
	public void evictExpired() {
		var now = Instant.now();
		byRefreshToken.entrySet().removeIf(e -> {
			var expired = now.isAfter(e.getValue().expiresAt().plus(REFRESH_TOKEN_TTL));
			if (expired) {
				LOG.debug("Evicted expired session for userId: {}", e.getValue().userId());
			}
			return expired;
		});
	}

	public String jwtSecret() {
		return jwtSecret;
	}

	public Duration accessTokenTtl() {
		return ACCESS_TOKEN_TTL;
	}

	private static KeyPair generateRsaKeyPair() {
		try {
			var gen = KeyPairGenerator.getInstance("RSA");
			gen.initialize(RSA_KEY_SIZE, new SecureRandom());
			return gen.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("RSA not available", e);
		}
	}
}
