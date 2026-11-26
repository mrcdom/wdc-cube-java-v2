package br.com.wdc.store.persistence.impl.security;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Minimalist JWT utility using HMAC-SHA256.
 * <p>
 * Uses Gson for JSON handling (available in the persistence module).
 */
public final class JwtUtil {

	private static final String ALGORITHM = "HmacSHA256";
	private static final Base64.Encoder B64_ENC = Base64.getUrlEncoder().withoutPadding();
	private static final Base64.Decoder B64_DEC = Base64.getUrlDecoder();

	private static final String HEADER_JSON = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
	private static final String ENCODED_HEADER = B64_ENC.encodeToString(HEADER_JSON.getBytes(StandardCharsets.UTF_8));

	private JwtUtil() {
	}

	/**
	 * Creates a JWT with the claims sid (the session; {@code null} for a token of no session, the persistent one),
	 * sub (userId), usr (userName) and exp (expiration).
	 */
	public static String create(String sessionId, Long userId, String userName, Duration ttl, String secret) {
		var now = Instant.now();
		var exp = now.plus(ttl);

		var payload = new JsonObject();
		if (sessionId != null) {
			payload.addProperty("sid", sessionId);
		}
		payload.addProperty("sub", userId);
		payload.addProperty("usr", userName);
		payload.addProperty("iat", now.getEpochSecond());
		payload.addProperty("exp", exp.getEpochSecond());

		var encodedPayload = B64_ENC.encodeToString(payload.toString().getBytes(StandardCharsets.UTF_8));
		var signingInput = ENCODED_HEADER + "." + encodedPayload;
		var signature = sign(signingInput, secret);

		return signingInput + "." + signature;
	}

	/**
	 * Validates the JWT and returns the claims. Returns null if invalid or expired.
	 */
	public static Claims validate(String token, String secret) {
		if (token == null || token.isBlank()) {
			return null;
		}

		var parts = token.split("\\.");
		if (parts.length != 3) {
			return null;
		}

		var signingInput = parts[0] + "." + parts[1];
		var expectedSignature = sign(signingInput, secret);

		// Constant-time comparison to prevent timing attacks
		if (!MessageDigest.isEqual(
				expectedSignature.getBytes(StandardCharsets.UTF_8),
				parts[2].getBytes(StandardCharsets.UTF_8))) {
			return null;
		}

		try {
			var payloadJson = new String(B64_DEC.decode(parts[1]), StandardCharsets.UTF_8);
			var payload = JsonParser.parseString(payloadJson).getAsJsonObject();

			var exp = payload.get("exp").getAsLong();
			if (Instant.now().getEpochSecond() > exp) {
				return null;
			}

			return new Claims(
					payload.has("sid") ? payload.get("sid").getAsString() : null,
					payload.get("sub").getAsLong(),
					payload.has("usr") ? payload.get("usr").getAsString() : null,
					Instant.ofEpochSecond(exp));
		} catch (Exception e) {
			return null;
		}
	}

	private static String sign(String data, String secret) {
		try {
			var mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
			return B64_ENC.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException | InvalidKeyException e) {
			throw new AssertionError("HMAC-SHA256 not available", e);
		}
	}

	/**
	 * Claims extracted from a valid JWT; {@code sessionId} is {@code null} for a token of no session.
	 */
	public record Claims(String sessionId, Long userId, String userName, Instant expiresAt) {
	}
}
