package br.com.wdc.store.persistence.impl.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.domain.security.Scram;
import br.com.wdc.framework.domain.security.ScramVerifier;
import br.com.wdc.framework.domain.security.SecurityContext;
import br.com.wdc.store.domain.security.Role;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;

/**
 * Signs users in with SCRAM-SHA-256 ({@link Scram}) against the verifier each user has in the database, and keeps the
 * sessions ({@link AccessContextCache}) and their JWTs. Reads the credentials, the verifier included, from the user
 * repository itself — the data policies guard the REST API, not the repositories.
 * <p>
 * The answers do not tell whether a user exists: an unknown user gets a challenge with made-up parameters, the same
 * for the same name, and a refusal costs the same work. Failed sign-ins on one account are slowed down
 * ({@link LoginThrottle}), whichever way they come in.
 */
public final class AuthenticationServiceImpl implements AuthenticationService {

	private static final Log LOG = Log.getLogger(AuthenticationServiceImpl.class);
	private static final String HMAC_ALGORITHM = "HmacSHA256";
	private static final Duration PERSISTENT_TOKEN_TTL = Duration.ofDays(30);

	private final UserRepository rawUserRepo;
	private final AccessContextCache cache;
	private final NonceStore nonceStore;
	private final LoginThrottle throttle;
	private final Scram scram = new Scram(new JceCryptoProvider());

	/** The key of the made-up salts: derived from the JWT secret, so they stay the same across restarts. */
	private final byte[] decoySaltKey;

	/** What an unknown user's proof is checked against, so a refusal takes the same work either way. */
	private final ScramVerifier decoyVerifier = scram.verifier("decoy", new byte[ScramVerifier.SALT_SIZE], 1);

	public AuthenticationServiceImpl(UserRepository rawUserRepo, String jwtSecret) {
		this(rawUserRepo, jwtSecret, new LoginThrottle());
	}

	public AuthenticationServiceImpl(UserRepository rawUserRepo, String jwtSecret, LoginThrottle throttle) {
		this.rawUserRepo = rawUserRepo;
		this.cache = new AccessContextCache(jwtSecret);
		this.nonceStore = new NonceStore();
		this.throttle = throttle;
		this.decoySaltKey = hmac("wdc-store-scram-decoy".getBytes(StandardCharsets.UTF_8),
				jwtSecret.getBytes(StandardCharsets.UTF_8));

		// Schedules periodic cleanup of expired sessions, nonces and forgotten sign-in failures.
		// A 1h interval is enough: refresh TTL is 7 days, nonce TTL is 1 min, failures are forgotten after 1 h.
		var scheduler = ScheduledExecutor.BEAN.get();
		if (scheduler != null) {
			scheduler.scheduleAtFixedRate(this::evictExpired, Duration.ofMinutes(10), Duration.ofHours(1));
		}
	}

	@Override
	public ChallengeResult challenge(String userName) {
		var verifier = userName == null ? null : ScramVerifier.parse(passwordOf(findUser(userName)));
		var salt = verifier != null ? verifier.salt() : decoySalt(userName == null ? "" : userName);
		var iterations = verifier != null ? verifier.iterations() : ScramVerifier.DEFAULT_ITERATIONS;
		var nonce = nonceStore.generate(userName);
		return new ChallengeResult(nonce, Base64.getEncoder().encodeToString(salt), iterations,
				nonceStore.expiresAt(nonce));
	}

	@Override
	public AuthResult login(String userName, String proof, String nonce) {
		if (userName == null || proof == null || nonce == null) {
			return null;
		}

		throttle.check(userName);

		var user = findUser(userName);
		var verifier = ScramVerifier.parse(passwordOf(user));
		var authMessage = Scram.authMessage(userName, nonce);
		byte[] proofBytes;
		try {
			proofBytes = Base64.getDecoder().decode(proof);
		} catch (IllegalArgumentException notBase64) {
			proofBytes = new byte[0];
		}
		// the nonce and the proof are both checked, and the proof even without a user: the same work for every refusal
		var nonceOk = nonceStore.consume(nonce, userName);
		var proofOk = scram.verifyProof(verifier != null ? verifier : decoyVerifier, authMessage, proofBytes);

		if (user == null || verifier == null || !nonceOk || !proofOk) {
			throttle.failed(userName);
			LOG.warn("Login refused: {}", nonceOk ? "wrong credentials" : "invalid, expired or foreign nonce");
			return null;
		}
		throttle.succeeded(userName);

		var roles = Role.parse(user.roles());
		var session = cache.createSession(user.id(), user.userName(), Role.names(roles));
		var accessToken = JwtUtil.create(session.sessionId(), user.id(), user.userName(), cache.accessTokenTtl(),
				cache.jwtSecret());
		var serverSignature = Base64.getEncoder().encodeToString(scram.serverSignature(verifier.serverKey(), authMessage));

		return new AuthResult(user.id(), accessToken, session.refreshToken(), session.expiresAt(), session.publicKeyBase64(),
				serverSignature);
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

		var accessToken = JwtUtil.create(session.sessionId(), session.userId(), session.userName(),
				cache.accessTokenTtl(), cache.jwtSecret());

		// a refresh proves nothing about the password: no server signature
		return new AuthResult(session.userId(), accessToken, session.refreshToken(), session.expiresAt(),
				session.publicKeyBase64(), null);
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
		// only an access token carries a session: the persistent one does not open the API
		return claims.sessionId() == null ? null : cache.getBySessionId(claims.sessionId());
	}

	@Override
	public String createPersistentToken(Long userId, String userName) {
		return JwtUtil.create(null, userId, userName, PERSISTENT_TOKEN_TTL, cache.jwtSecret());
	}

	@Override
	public AuthResult loginWithPersistentToken(String persistentToken) {
		if (persistentToken == null || persistentToken.isBlank()) {
			return null;
		}

		var claims = JwtUtil.validate(persistentToken, cache.jwtSecret());
		// an access token (it has a session) is not a persistent token
		if (claims == null || claims.sessionId() != null) {
			LOG.debug("Persistent token validation failed");
			return null;
		}

		// Fetch user to verify still exists and get roles
		var user = fetchUserById(claims.userId());
		if (user == null) {
			LOG.warn("Persistent token login failed: user {} no longer exists", claims.userId());
			return null;
		}

		var roles = Role.parse(user.roles());
		var session = cache.createSession(user.id(), user.userName(), Role.names(roles));
		var accessToken = JwtUtil.create(session.sessionId(), user.id(), user.userName(), cache.accessTokenTtl(),
				cache.jwtSecret());

		LOG.info("Auto-login via persistent token for user: {} ({})", user.userName(), user.id());
		return new AuthResult(user.id(), accessToken, session.refreshToken(), session.expiresAt(), session.publicKeyBase64(),
				null);
	}

	// :: Internal

	/** Drops the expired sessions, the challenges nobody answered and the forgotten failures. */
	public void evictExpired() {
		try {
			cache.evictExpired();
			nonceStore.evictExpired();
			throttle.evictExpired();
		} catch (RuntimeException e) {
			LOG.error("Evicting expired sessions", e);
		}
	}

	/** The user's id, login, verifier and roles; {@code null} when there is no such user. */
	private User findUser(String userName) {
		var pv = ProjectionValues.INSTANCE;
		var prj = new User()
				.withId(pv.i64)
				.withUserName(pv.str)
				.withPassword(pv.str)
				.withRoles(pv.str);

		var users = rawUserRepo.fetch(new UserCriteria()
				.withUserName(userName)
				.withProjection(prj), 0, 1);

		return users.isEmpty() ? null : users.get(0);
	}

	private static String passwordOf(User user) {
		return user == null ? null : user.password();
	}

	/** A made-up salt for a user that does not exist: the same for the same name, unrelated to any real one. */
	private byte[] decoySalt(String userName) {
		var mac = hmac(decoySaltKey, userName.trim().toLowerCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8));
		var salt = new byte[ScramVerifier.SALT_SIZE];
		System.arraycopy(mac, 0, salt, 0, salt.length);
		return salt;
	}

	private User fetchUserById(Long userId) {
		var pv = ProjectionValues.INSTANCE;
		var prj = new User()
				.withId(pv.i64)
				.withUserName(pv.str)
				.withRoles(pv.str);

		var users = rawUserRepo.fetch(new UserCriteria()
				.withUserId(userId)
				.withProjection(prj), 0, 1);

		return users.isEmpty() ? null : users.get(0);
	}

	private static byte[] hmac(byte[] key, byte[] data) {
		try {
			var mac = Mac.getInstance(HMAC_ALGORITHM);
			mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
			return mac.doFinal(data);
		} catch (Exception e) {
			throw new AssertionError("HMAC-SHA256 failed", e);
		}
	}
}
