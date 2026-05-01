package br.com.wdc.shopping.persistence.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache em memória de sessões autenticadas.
 * <p>
 * Indexa por userId — um usuário tem no máximo uma sessão ativa.
 */
public final class AccessContextCache {

	private static final Logger LOG = LoggerFactory.getLogger(AccessContextCache.class);

	private static final int RSA_KEY_SIZE = 2048;
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

	private final ConcurrentHashMap<Long, AccessContext> byUserId = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, Long> refreshTokenIndex = new ConcurrentHashMap<>();

	private final String jwtSecret;

	public AccessContextCache(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	/**
	 * Cria uma nova sessão para o usuário autenticado.
	 * Se já existia uma sessão anterior, ela é substituída.
	 */
	public AccessContext createSession(Long userId, String userName, Set<String> permissions) {
		var keyPair = generateRsaKeyPair();
		var refreshToken = UUID.randomUUID().toString();
		var expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);

		var ctx = new AccessContext(userId, userName, permissions, keyPair, expiresAt, refreshToken);

		var previous = byUserId.put(userId, ctx);
		if (previous != null) {
			refreshTokenIndex.remove(previous.refreshToken());
		}
		refreshTokenIndex.put(refreshToken, userId);

		LOG.info("Session created for user: {} ({})", userName, userId);
		return ctx;
	}

	/**
	 * Renova o access token usando o refresh token.
	 * Retorna null se o refresh token for inválido.
	 */
	public AccessContext refresh(String refreshToken) {
		var userId = refreshTokenIndex.get(refreshToken);
		if (userId == null) {
			return null;
		}

		var existing = byUserId.get(userId);
		if (existing == null || !existing.refreshToken().equals(refreshToken)) {
			refreshTokenIndex.remove(refreshToken);
			return null;
		}

		var newRefreshToken = UUID.randomUUID().toString();
		var expiresAt = Instant.now().plus(ACCESS_TOKEN_TTL);

		var ctx = new AccessContext(userId, existing.userName(), existing.permissions(),
				generateRsaKeyPair(), expiresAt, newRefreshToken);

		byUserId.put(userId, ctx);
		refreshTokenIndex.remove(refreshToken);
		refreshTokenIndex.put(newRefreshToken, userId);

		LOG.info("Session refreshed for user: {} ({})", existing.userName(), userId);
		return ctx;
	}

	/**
	 * Busca o contexto de acesso pelo userId.
	 */
	public AccessContext get(Long userId) {
		var ctx = byUserId.get(userId);
		if (ctx != null && ctx.isExpired()) {
			remove(userId);
			return null;
		}
		return ctx;
	}

	/**
	 * Remove (logout) a sessão do usuário.
	 */
	public void remove(Long userId) {
		var ctx = byUserId.remove(userId);
		if (ctx != null) {
			refreshTokenIndex.remove(ctx.refreshToken());
			LOG.info("Session removed for user: {}", userId);
		}
	}

	/**
	 * Remove a sessão associada ao refresh token.
	 *
	 * @return true se a sessão foi encontrada e removida
	 */
	public boolean removeByRefreshToken(String refreshToken) {
		var userId = refreshTokenIndex.get(refreshToken);
		if (userId == null) {
			return false;
		}
		remove(userId);
		return true;
	}

	/**
	 * Remove sessões expiradas. Chamado periodicamente.
	 */
	public void evictExpired() {
		var now = Instant.now();
		Iterator<Map.Entry<Long, AccessContext>> it = byUserId.entrySet().iterator();
		while (it.hasNext()) {
			var entry = it.next();
			if (now.isAfter(entry.getValue().expiresAt().plus(REFRESH_TOKEN_TTL))) {
				refreshTokenIndex.remove(entry.getValue().refreshToken());
				it.remove();
				LOG.debug("Evicted expired session for userId: {}", entry.getKey());
			}
		}
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
