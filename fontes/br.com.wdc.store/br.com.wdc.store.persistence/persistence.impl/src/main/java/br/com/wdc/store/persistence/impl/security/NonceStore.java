package br.com.wdc.store.persistence.impl.security;

import static br.com.wdc.store.persistence.impl.scheme.Tables.EN_AUTH_NONCE;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.jooq.DSLContext;

import br.com.wdc.store.persistence.impl.StoreDSLContext;

/**
 * The nonces of the sign-in challenges: each one is for one user, single-use, and expires after a short TTL.
 * <p>
 * Kept in the database ({@code EN_AUTH_NONCE}), not in one instance's memory: a challenge answered by one backend
 * instance and the sign-in sent to another find the same nonce, and it is used up once, whichever consumes it first.
 */
public final class NonceStore {

	private static final Duration NONCE_TTL = Duration.ofSeconds(60);

	/** A new nonce and when it expires. */
	public record Issued(String nonce, Instant expiresAt) {
	}

	/** Generates and stores a new nonce for {@code userName}'s sign-in. */
	public Issued generate(String userName) {
		var nonce = UUID.randomUUID().toString();
		// whole milliseconds: what the database keeps is what is answered
		var expiresAt = Instant.now().plus(NONCE_TTL).truncatedTo(ChronoUnit.MILLIS);
		dsl().insertInto(EN_AUTH_NONCE)
				.set(EN_AUTH_NONCE.NONCE, nonce)
				.set(EN_AUTH_NONCE.USERNAME, LoginThrottle.key(userName == null ? "" : userName))
				.set(EN_AUTH_NONCE.EXPIRES_AT, utc(expiresAt))
				.execute();
		return new Issued(nonce, expiresAt);
	}

	/**
	 * Consumes the nonce (single use, even when refused): {@code true} when it exists, has not expired and was made for
	 * {@code userName}. Of two consuming it at once, only the one whose delete took it says {@code true}.
	 */
	public boolean consume(String nonce, String userName) {
		if (nonce == null) {
			return false;
		}
		var row = dsl().select(EN_AUTH_NONCE.USERNAME, EN_AUTH_NONCE.EXPIRES_AT)
				.from(EN_AUTH_NONCE)
				.where(EN_AUTH_NONCE.NONCE.eq(nonce))
				.fetchOne();
		if (row == null) {
			return false;
		}
		var taken = dsl().deleteFrom(EN_AUTH_NONCE)
				.where(EN_AUTH_NONCE.NONCE.eq(nonce))
				.execute() == 1;
		return taken
				&& Instant.now().isBefore(row.value2().toInstant(ZoneOffset.UTC))
				&& userName != null
				&& row.value1().equals(LoginThrottle.key(userName));
	}

	/** Removes expired nonces. Called periodically. */
	public void evictExpired() {
		dsl().deleteFrom(EN_AUTH_NONCE)
				.where(EN_AUTH_NONCE.EXPIRES_AT.lt(utc(Instant.now())))
				.execute();
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static DSLContext dsl() {
		return StoreDSLContext.BEAN.get();
	}
}
