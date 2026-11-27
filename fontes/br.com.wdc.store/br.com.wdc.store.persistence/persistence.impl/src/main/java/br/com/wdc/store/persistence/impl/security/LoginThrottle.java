package br.com.wdc.store.persistence.impl.security;

import static br.com.wdc.store.persistence.impl.scheme.Tables.EN_LOGIN_FAILURE;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Locale;

import org.jooq.DSLContext;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;

import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.store.persistence.impl.StoreDSLContext;

/**
 * Slows down password guessing on one account, whatever the way in (the REST API or a view whose presenters run on the
 * server). After {@code freeFailures} failed sign-ins in a row, each new attempt waits — {@code firstWait}, doubling
 * with each failure, up to {@code maxWait}; a success clears the count. A wait, not a lock: the user signs in once it
 * passes, and no one can lock an account out for good by failing on purpose. Failures older than {@code forgetAfter}
 * are forgotten.
 * <p>
 * Kept in the database ({@code EN_LOGIN_FAILURE}: the count and the last failure, in UTC), not in one instance's
 * memory: with several backend instances, attempts spread over them count as one. The wait is worked out from those
 * two when an attempt is checked, so the only write instances can race on is the count, and that is one
 * {@code UPDATE} that adds one. It complements the per-address limit of {@code /api/auth}, which can't tell a crowd of
 * addresses aiming at one account.
 */
public final class LoginThrottle {

	/** The width of {@code USERNAME}: a longer name is kept by its hash. */
	private static final int MAX_KEY = 100;

	private final int freeFailures;
	private final Duration firstWait;
	private final Duration maxWait;
	private final Duration forgetAfter;
	private final Clock clock;

	/** 5 free failures, then 30 s doubling up to 15 min; failures forgotten after 1 h. */
	public LoginThrottle() {
		this(5, Duration.ofSeconds(30), Duration.ofMinutes(15), Duration.ofHours(1), Clock.systemUTC());
	}

	public LoginThrottle(int freeFailures, Duration firstWait, Duration maxWait, Duration forgetAfter, Clock clock) {
		this.freeFailures = freeFailures;
		this.firstWait = firstWait;
		this.maxWait = maxWait;
		this.forgetAfter = forgetAfter;
		this.clock = clock;
	}

	/** Refuses the attempt while {@code userName} must wait: {@link TooManyRequestsException} with the seconds left. */
	public void check(String userName) {
		var row = dsl().select(EN_LOGIN_FAILURE.FAILURES, EN_LOGIN_FAILURE.LAST_FAILURE)
				.from(EN_LOGIN_FAILURE)
				.where(EN_LOGIN_FAILURE.USERNAME.eq(key(userName)))
				.fetchOne();
		if (row == null) {
			return;
		}
		var last = row.value2().toInstant(ZoneOffset.UTC);
		var now = Instant.now(clock);
		if (last.plus(forgetAfter).isBefore(now)) {
			return;
		}
		var wait = waitAfter(row.value1());
		if (wait == null) {
			return;
		}
		var left = Duration.between(now, last.plus(wait));
		if (!left.isNegative() && !left.isZero()) {
			throw new TooManyRequestsException("Too many failed sign-ins: try again later", (left.toMillis() + 999) / 1000);
		}
	}

	public void failed(String userName) {
		var key = key(userName);
		var now = utc(Instant.now(clock));
		var cutoff = utc(Instant.now(clock).minus(forgetAfter));
		// add one (or start over, past forgetAfter); the first failure inserts, and one inserted meanwhile by another
		// instance is counted on the next round
		for (int attempt = 0; attempt < 3; attempt++) {
			int updated = dsl().update(EN_LOGIN_FAILURE)
					.set(EN_LOGIN_FAILURE.FAILURES, DSL.when(EN_LOGIN_FAILURE.LAST_FAILURE.lt(cutoff), DSL.inline(1))
							.otherwise(EN_LOGIN_FAILURE.FAILURES.plus(1)))
					.set(EN_LOGIN_FAILURE.LAST_FAILURE, now)
					.where(EN_LOGIN_FAILURE.USERNAME.eq(key))
					.execute();
			if (updated == 1) {
				return;
			}
			try {
				dsl().insertInto(EN_LOGIN_FAILURE)
						.set(EN_LOGIN_FAILURE.USERNAME, key)
						.set(EN_LOGIN_FAILURE.FAILURES, 1)
						.set(EN_LOGIN_FAILURE.LAST_FAILURE, now)
						.execute();
				return;
			} catch (DataAccessException insertedByAnotherInstance) {
				// another instance inserted it first: update it on the next round
			}
		}
	}

	public void succeeded(String userName) {
		dsl().deleteFrom(EN_LOGIN_FAILURE)
				.where(EN_LOGIN_FAILURE.USERNAME.eq(key(userName)))
				.execute();
	}

	/** Drops the failures already forgotten. */
	public void evictExpired() {
		var cutoff = utc(Instant.now(clock).minus(forgetAfter));
		dsl().deleteFrom(EN_LOGIN_FAILURE)
				.where(EN_LOGIN_FAILURE.LAST_FAILURE.lt(cutoff))
				.execute();
	}

	/** The wait after {@code failures} in a row: {@code null} while they are free. */
	private Duration waitAfter(int failures) {
		int over = failures - freeFailures;
		if (over <= 0) {
			return null;
		}
		var wait = firstWait.multipliedBy(1L << Math.min(over - 1, 20));
		return wait.compareTo(maxWait) > 0 ? maxWait : wait;
	}

	private static DSLContext dsl() {
		return StoreDSLContext.BEAN.get();
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	/** How a user name is kept: trimmed, in lower case, a longer one than the column by its SHA-256. */
	static String key(String userName) {
		var key = userName.trim().toLowerCase(Locale.ROOT);
		if (key.length() <= MAX_KEY) {
			return key;
		}
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
			return "#" + HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("SHA-256 not available", e);
		}
	}
}
