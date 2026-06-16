package br.com.wdc.shopping.persistence.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.domain.criteria.UserCriteria;
import br.com.wdc.shopping.domain.model.User;
import br.com.wdc.shopping.domain.repositories.UserRepository;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.Role;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.framework.domain.projection.ProjectionValues;

/**
 * Implementação de {@link AuthenticationService}.
 * <p>
 * Gerencia o fluxo HMAC challenge-response, sessões em memória e JWT.
 * Usa o repositório de usuários <b>não-decorado</b> (raw) para buscar
 * credenciais sem passar por verificação de permissão.
 */
public final class AuthenticationServiceImpl implements AuthenticationService {

	private static final Log LOG = Log.getLogger(AuthenticationServiceImpl.class);
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Duration PERSISTENT_TOKEN_TTL = Duration.ofDays(30);

	private final UserRepository rawUserRepo;
	private final AccessContextCache cache;
	private final NonceStore nonceStore;

	public AuthenticationServiceImpl(UserRepository rawUserRepo, String jwtSecret) {
		this.rawUserRepo = rawUserRepo;
		this.cache = new AccessContextCache(jwtSecret);
		this.nonceStore = new NonceStore();

		// Agenda limpeza periódica de sessões e nonces expirados.
		// Intervalo de 1h é suficiente: refresh TTL é 7 dias, nonce TTL é 5 min.
		var scheduler = ScheduledExecutor.BEAN.get();
		if (scheduler != null) {
			scheduler.scheduleAtFixedRate(() -> {
				cache.evictExpired();
				nonceStore.evictExpired();
			}, Duration.ofMinutes(10), Duration.ofHours(1));
		}
	}

	@Override
	public ChallengeResult challenge() {
		var nonce = nonceStore.generate();
		var expiresAt = nonceStore.expiresAt(nonce);
		return new ChallengeResult(nonce, expiresAt);
	}

	@Override
	public AuthResult login(String userName, String digest, String nonce) {
		if (userName == null || digest == null || nonce == null) {
			return null;
		}

		if (!nonceStore.consume(nonce)) {
			LOG.warn("Login failed: invalid or expired nonce");
			return null;
		}

		var user = fetchUserForAuth(userName);
		if (user == null) {
			LOG.warn("Login failed: user not found: {}", userName);
			return null;
		}

		var expectedDigest = computeHmac(user.password, userName + nonce);

		if (!MessageDigest.isEqual(
				expectedDigest.getBytes(StandardCharsets.UTF_8),
				digest.getBytes(StandardCharsets.UTF_8))) {
			LOG.warn("Login failed: HMAC mismatch for user: {}", userName);
			return null;
		}

		var roles = Role.parse(user.roles);
		var permissions = Role.effectivePermissions(roles);
		var session = cache.createSession(user.id, user.userName, permissions);
		var accessToken = JwtUtil.create(user.id, user.userName, cache.accessTokenTtl(), cache.jwtSecret());

		return new AuthResult(user.id, accessToken, session.refreshToken(), session.expiresAt(), session.publicKeyBase64());
	}

	@Override
	public AuthResult refresh(String refreshToken) {
		if (refreshToken == null) {
			return null;
		}

		var session = cache.refresh(refreshToken);
		if (session == null) {
			return null;
		}

		var accessToken = JwtUtil.create(session.userId(), session.userName(),
				cache.accessTokenTtl(), cache.jwtSecret());

		return new AuthResult(session.userId(), accessToken, session.refreshToken(), session.expiresAt(), session.publicKeyBase64());
	}

	@Override
	public void logout(String refreshToken) {
		if (refreshToken != null) {
			cache.removeByRefreshToken(refreshToken);
		}
	}

	@Override
	public SecurityContext resolveToken(String jwtToken) {
		var claims = JwtUtil.validate(jwtToken, cache.jwtSecret());
		if (claims == null) {
			return null;
		}
		return cache.get(claims.userId());
	}

	@Override
	public String createPersistentToken(Long userId, String userName) {
		return JwtUtil.create(userId, userName, PERSISTENT_TOKEN_TTL, cache.jwtSecret());
	}

	@Override
	public AuthResult loginWithPersistentToken(String persistentToken) {
		if (persistentToken == null || persistentToken.isBlank()) {
			return null;
		}

		var claims = JwtUtil.validate(persistentToken, cache.jwtSecret());
		if (claims == null) {
			LOG.debug("Persistent token validation failed");
			return null;
		}

		// Fetch user to verify still exists and get roles
		var user = fetchUserById(claims.userId());
		if (user == null) {
			LOG.warn("Persistent token login failed: user {} no longer exists", claims.userId());
			return null;
		}

		var roles = Role.parse(user.roles);
		var permissions = Role.effectivePermissions(roles);
		var session = cache.createSession(user.id, user.userName, permissions);
		var accessToken = JwtUtil.create(user.id, user.userName, cache.accessTokenTtl(), cache.jwtSecret());

		LOG.info("Auto-login via persistent token for user: {} ({})", user.userName, user.id);
		return new AuthResult(user.id, accessToken, session.refreshToken(), session.expiresAt(), session.publicKeyBase64());
	}

	// :: Internal

	private User fetchUserForAuth(String userName) {
		var pv = ProjectionValues.INSTANCE;
		var prj = new User();
		prj.id = pv.i64;
		prj.userName = pv.str;
		prj.password = pv.str;
		prj.roles = pv.str;

		var users = rawUserRepo.fetch(new UserCriteria()
				.withUserName(userName)
				.withProjection(prj), 0, 1);

		if (users.isEmpty()) {
			return null;
		}

		var user = users.get(0);
		// CHAR(32) column pads with trailing spaces; trim for exact HMAC comparison
		if (user.password != null) {
			user.password = user.password.trim();
		}
		return user;
	}

	private User fetchUserById(Long userId) {
		var pv = ProjectionValues.INSTANCE;
		var prj = new User();
		prj.id = pv.i64;
		prj.userName = pv.str;
		prj.roles = pv.str;

		var users = rawUserRepo.fetch(new UserCriteria()
				.withUserId(userId)
				.withProjection(prj), 0, 1);

		return users.isEmpty() ? null : users.get(0);
	}

	private static String computeHmac(String key, String data) {
		try {
			var mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
			var hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(hash);
		} catch (Exception e) {
			throw new AssertionError("HMAC-SHA256 failed", e);
		}
	}

	private static String bytesToHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
}
