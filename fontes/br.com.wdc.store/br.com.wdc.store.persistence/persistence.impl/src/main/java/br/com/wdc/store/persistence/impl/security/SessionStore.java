package br.com.wdc.store.persistence.impl.security;

import static br.com.wdc.store.persistence.impl.scheme.Tables.EN_USER_SESSION;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jooq.DSLContext;
import org.jooq.Record;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.store.persistence.impl.StoreDSLContext;

/**
 * The sessions in the database ({@code EN_USER_SESSION}), so that every backend instance knows the sessions made on any
 * of them: who, the roles, the refresh token, when the access token expires (UTC), and the session's RSA key pair.
 * <p>
 * <b>The private key is stored encrypted</b> (AES-256-GCM, {@code v1:} + Base64 of the IV and the ciphertext), with a
 * key derived from the JWT secret: a copy of the table alone does not give the sessions' keys. A row that does not
 * decrypt with this secret is treated as missing, and its client signs in again.
 */
final class SessionStore {

	private static final Log LOG = Log.getLogger(SessionStore.class);

	private static final String ENCRYPTED_PREFIX = "v1:";
	private static final int GCM_IV_BYTES = 12;
	private static final int GCM_TAG_BITS = 128;

	private final SecretKeySpec keyCipher;
	private final SecureRandom random = new SecureRandom();

	SessionStore(String jwtSecret) {
		try {
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
			this.keyCipher = new SecretKeySpec(
					mac.doFinal("wdc-store/session-private-key/v1".getBytes(StandardCharsets.UTF_8)), "AES");
		} catch (GeneralSecurityException e) {
			throw new AssertionError("HMAC-SHA256 not available", e);
		}
	}

	void insert(AccessContext session) {
		var t = EN_USER_SESSION;
		dsl().insertInto(t)
				.set(t.SESSION_ID, session.sessionId())
				.set(t.USERID, session.userId())
				.set(t.USERNAME, session.userName())
				.set(t.ROLES, String.join(",", session.roles()))
				.set(t.REFRESH_TOKEN, session.refreshToken())
				.set(t.EXPIRES_AT, utc(session.expiresAt()))
				.set(t.PUBLIC_KEY, encode(session.publicKey().getEncoded()))
				.set(t.PRIVATE_KEY, encrypt(session.privateKey().getEncoded()))
				.execute();
	}

	/**
	 * Stores the renewal of a session, only if its refresh token is still {@code oldRefreshToken}: of two instances
	 * renewing the same token at once, one wins. {@code true} when this one did.
	 */
	boolean renew(AccessContext renewed, String oldRefreshToken) {
		var t = EN_USER_SESSION;
		return dsl().update(t)
				.set(t.REFRESH_TOKEN, renewed.refreshToken())
				.set(t.EXPIRES_AT, utc(renewed.expiresAt()))
				.set(t.PUBLIC_KEY, encode(renewed.publicKey().getEncoded()))
				.set(t.PRIVATE_KEY, encrypt(renewed.privateKey().getEncoded()))
				.where(t.SESSION_ID.eq(renewed.sessionId()))
				.and(t.REFRESH_TOKEN.eq(oldRefreshToken))
				.execute() == 1;
	}

	AccessContext findBySessionId(String sessionId) {
		return map(dsl().selectFrom(EN_USER_SESSION)
				.where(EN_USER_SESSION.SESSION_ID.eq(sessionId))
				.fetchOne());
	}

	AccessContext findByRefreshToken(String refreshToken) {
		return map(dsl().selectFrom(EN_USER_SESSION)
				.where(EN_USER_SESSION.REFRESH_TOKEN.eq(refreshToken))
				.fetchOne());
	}

	/** {@code true} when there was such a session. */
	boolean delete(String sessionId) {
		return dsl().deleteFrom(EN_USER_SESSION)
				.where(EN_USER_SESSION.SESSION_ID.eq(sessionId))
				.execute() > 0;
	}

	/** Drops the sessions whose access token expired before {@code cutoff}. */
	int deleteExpiredBefore(Instant cutoff) {
		return dsl().deleteFrom(EN_USER_SESSION)
				.where(EN_USER_SESSION.EXPIRES_AT.lt(utc(cutoff)))
				.execute();
	}

	// :: Internals

	private AccessContext map(Record row) {
		if (row == null) {
			return null;
		}
		var t = EN_USER_SESSION;
		var privateKey = decrypt(row.get(t.PRIVATE_KEY));
		if (privateKey == null) {
			LOG.warn("Session {} not restored: its key does not decrypt with this secret", row.get(t.SESSION_ID));
			return null;
		}
		try {
			var keys = KeyFactory.getInstance("RSA");
			var keyPair = new KeyPair(
					keys.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(row.get(t.PUBLIC_KEY)))),
					keys.generatePrivate(new PKCS8EncodedKeySpec(privateKey)));
			return new AccessContext(row.get(t.SESSION_ID), row.get(t.USERID), row.get(t.USERNAME),
					roles(row.get(t.ROLES)), keyPair, row.get(t.EXPIRES_AT).toInstant(ZoneOffset.UTC),
					row.get(t.REFRESH_TOKEN));
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			LOG.warn("Session {} not restored: unreadable key", row.get(t.SESSION_ID));
			return null;
		} finally {
			Arrays.fill(privateKey, (byte) 0);
		}
	}

	private static Set<String> roles(String stored) {
		var roles = new LinkedHashSet<String>();
		if (stored != null) {
			for (var role : stored.split(",")) {
				if (!role.isBlank()) {
					roles.add(role.trim());
				}
			}
		}
		return Set.copyOf(roles);
	}

	private String encrypt(byte[] plain) {
		try {
			var iv = new byte[GCM_IV_BYTES];
			random.nextBytes(iv);
			var cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, keyCipher, new GCMParameterSpec(GCM_TAG_BITS, iv));
			var sealed = cipher.doFinal(plain);
			return ENCRYPTED_PREFIX + encode(ByteBuffer.allocate(iv.length + sealed.length)
					.put(iv)
					.put(sealed)
					.array());
		} catch (GeneralSecurityException e) {
			throw new AssertionError("AES-GCM not available", e);
		}
	}

	/** The private key's bytes; {@code null} when it does not decrypt with this secret. */
	private byte[] decrypt(String stored) {
		if (stored == null || !stored.startsWith(ENCRYPTED_PREFIX)) {
			return null;
		}
		try {
			var bytes = Base64.getDecoder().decode(stored.substring(ENCRYPTED_PREFIX.length()));
			var cipher = Cipher.getInstance("AES/GCM/NoPadding");
			cipher.init(Cipher.DECRYPT_MODE, keyCipher, new GCMParameterSpec(GCM_TAG_BITS, bytes, 0, GCM_IV_BYTES));
			return cipher.doFinal(bytes, GCM_IV_BYTES, bytes.length - GCM_IV_BYTES);
		} catch (GeneralSecurityException | IllegalArgumentException e) {
			return null;
		}
	}

	private static String encode(byte[] bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	private static LocalDateTime utc(Instant instant) {
		return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
	}

	private static DSLContext dsl() {
		return StoreDSLContext.BEAN.get();
	}
}
