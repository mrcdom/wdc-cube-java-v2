package br.com.wdc.shopping.persistence.impl.security;

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
 * Cache em memória de sessões autenticadas.
 * <p>
 * Suporta múltiplas sessões ativas por usuário (multi-device).
 * Indexa por refreshToken — cada dispositivo/aba mantém sua própria sessão.
 */
public final class AccessContextCache {

	private static final Log LOG = Log.getLogger(AccessContextCache.class);

	private static final int RSA_KEY_SIZE = 2048;
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

	/** Índice primário: refreshToken → AccessContext. */
	private final ConcurrentHashMap<String, AccessContext> byRefreshToken = new ConcurrentHashMap<>();

	private final String jwtSecret;

	public AccessContextCache(String jwtSecret) {
		this.jwtSecret = jwtSecret;
	}

	/**
	 * Cria uma nova sessão para o usuário autenticado.
	 * Sessões anteriores do mesmo usuário <em>não</em> são removidas —
	 * cada dispositivo tem sua sessão independente.
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
	 * Renova o access token usando o refresh token.
	 * Retorna null se o refresh token for inválido ou expirado.
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
	 * Busca qualquer sessão válida (não expirada) para o userId.
	 * Usado após validação de JWT para obter permissões e chave RSA.
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
	 * Remove (logout) <em>todas</em> as sessões do usuário.
	 */
	public void remove(Long userId) {
		byRefreshToken.entrySet().removeIf(e -> userId.equals(e.getValue().userId()));
		LOG.info("All sessions removed for userId: {}", userId);
	}

	/**
	 * Remove apenas a sessão associada ao refresh token (logout de um dispositivo).
	 *
	 * @return true se a sessão foi encontrada e removida
	 */
	public boolean removeByRefreshToken(String refreshToken) {
		return byRefreshToken.remove(refreshToken) != null;
	}

	/**
	 * Remove sessões cujo refresh token expirou. Chamado periodicamente.
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
