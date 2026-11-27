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
import java.util.concurrent.atomic.AtomicLong;

import br.com.wdc.framework.commons.log.Log;

/**
 * The users' sessions: kept in the database ({@link SessionStore}), so that any backend instance answers for a session
 * made on another, and held in this instance's memory once read, so that a request reads the database only for a
 * session this instance does not hold. Each device or tab has its own session.
 * <p>
 * A session changed here — renewed, signed out, expired — is told to the other instances ({@link SessionEvents}), which
 * drop their copy; when notices may have been missed, every copy is dropped. A copy whose access token expired is read
 * again, since another instance may have renewed it.
 */
public final class AccessContextCache {

	private static final Log LOG = Log.getLogger(AccessContextCache.class);

	private static final int RSA_KEY_SIZE = 2048;
	private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(30);
	private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

	private final ConcurrentHashMap<String, AccessContext> bySessionId = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<String, String> sessionIdByRefreshToken = new ConcurrentHashMap<>();

	/**
	 * Counts the copies dropped by notices: a session read from the database is held only if none came while it was
	 * read, since the copy read may be the one the notice was about.
	 */
	private final AtomicLong dropped = new AtomicLong();

	private final String jwtSecret;
	private final SessionStore store;
	private final SessionEvents events;

	public AccessContextCache(String jwtSecret, SessionEvents events) {
		this.jwtSecret = jwtSecret;
		this.store = new SessionStore(jwtSecret);
		this.events = events;
		events.listen(this::changedElsewhere, this::missedNotices);
	}

	/**
	 * Creates a new session for the authenticated user. Earlier sessions of the same user are <em>not</em> removed —
	 * each device has its own independent session.
	 */
	public AccessContext createSession(Long userId, String userName, Set<String> roles) {
		var ctx = new AccessContext(UUID.randomUUID().toString(), userId, userName, roles, generateRsaKeyPair(),
				Instant.now().plus(ACCESS_TOKEN_TTL), UUID.randomUUID().toString());
		store.insert(ctx);
		hold(ctx);

		LOG.info("Session created for user: {} ({})", userName, userId);
		return ctx;
	}

	/**
	 * Renews the access token using the refresh token: the same session, with a new refresh token and a new key. Returns
	 * null if the refresh token is unknown, already used, or past its window.
	 */
	public AccessContext refresh(String refreshToken) {
		var existing = heldByRefreshToken(refreshToken);
		if (existing == null) {
			existing = store.findByRefreshToken(refreshToken);
		}
		if (existing == null || !refreshToken.equals(existing.refreshToken())) {
			return null;
		}
		if (Instant.now().isAfter(existing.expiresAt().plus(REFRESH_TOKEN_TTL))) {
			end(existing.sessionId());
			return null;
		}

		var renewed = new AccessContext(existing.sessionId(), existing.userId(), existing.userName(), existing.roles(),
				generateRsaKeyPair(), Instant.now().plus(ACCESS_TOKEN_TTL), UUID.randomUUID().toString());
		if (!store.renew(renewed, refreshToken)) {
			// renewed meanwhile, here or on another instance: this refresh token is spent
			drop(existing.sessionId());
			return null;
		}
		drop(existing.sessionId());
		hold(renewed);
		events.sessionChanged(renewed.sessionId());

		LOG.info("Session refreshed for user: {} ({})", existing.userName(), existing.userId());
		return renewed;
	}

	/**
	 * The live (unexpired) session of the id an access token carries: its roles and its RSA key. From memory when this
	 * instance holds it; otherwise, or when the copy held expired, from the database.
	 */
	public AccessContext getBySessionId(String sessionId) {
		var held = bySessionId.get(sessionId);
		if (held != null && !held.isExpired()) {
			return held;
		}
		if (held != null) {
			drop(sessionId);
		}

		var droppedBefore = dropped.get();
		var stored = store.findBySessionId(sessionId);
		if (stored == null || stored.isExpired()) {
			return null;
		}
		if (dropped.get() == droppedBefore) {
			hold(stored);
		}
		return stored;
	}

	/**
	 * Ends the session tied to the refresh token (logout of one device), here and on the other instances.
	 *
	 * @return true if the session was found and removed
	 */
	public boolean removeByRefreshToken(String refreshToken) {
		var sessionId = sessionIdByRefreshToken.get(refreshToken);
		if (sessionId == null) {
			var stored = store.findByRefreshToken(refreshToken);
			if (stored == null) {
				return false;
			}
			sessionId = stored.sessionId();
		}
		return end(sessionId);
	}

	/**
	 * Lets go of the copies whose access token expired (a request for one reads it again), and drops from the database
	 * the sessions whose refresh token can no longer be used. Called periodically.
	 */
	public void evictExpired() {
		for (var ctx : bySessionId.values()) {
			if (ctx.isExpired()) {
				drop(ctx.sessionId());
			}
		}
		var deleted = store.deleteExpiredBefore(Instant.now().minus(REFRESH_TOKEN_TTL));
		if (deleted > 0) {
			LOG.debug("Deleted {} sessions past their refresh window", deleted);
		}
	}

	/** The sessions held in memory now. */
	public int size() {
		return bySessionId.size();
	}

	public String jwtSecret() {
		return jwtSecret;
	}

	public Duration accessTokenTtl() {
		return ACCESS_TOKEN_TTL;
	}

	// :: Internals

	private AccessContext heldByRefreshToken(String refreshToken) {
		var sessionId = sessionIdByRefreshToken.get(refreshToken);
		return sessionId == null ? null : bySessionId.get(sessionId);
	}

	private void hold(AccessContext ctx) {
		var previous = bySessionId.put(ctx.sessionId(), ctx);
		if (previous != null && !previous.refreshToken().equals(ctx.refreshToken())) {
			sessionIdByRefreshToken.remove(previous.refreshToken(), ctx.sessionId());
		}
		sessionIdByRefreshToken.put(ctx.refreshToken(), ctx.sessionId());
	}

	private void drop(String sessionId) {
		var ctx = bySessionId.remove(sessionId);
		if (ctx != null) {
			sessionIdByRefreshToken.remove(ctx.refreshToken(), sessionId);
		}
	}

	/** Ends the session everywhere: memory, database, the other instances. */
	private boolean end(String sessionId) {
		drop(sessionId);
		var deleted = store.delete(sessionId);
		events.sessionChanged(sessionId);
		LOG.info("Session ended: {}", sessionId);
		return deleted;
	}

	private void changedElsewhere(String sessionId) {
		dropped.incrementAndGet();
		SecurityMeters.CHANGED_ELSEWHERE.increment();
		drop(sessionId);
	}

	private void missedNotices() {
		dropped.incrementAndGet();
		bySessionId.clear();
		sessionIdByRefreshToken.clear();
		LOG.info("Session copies dropped: notices from other instances may have been missed");
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
