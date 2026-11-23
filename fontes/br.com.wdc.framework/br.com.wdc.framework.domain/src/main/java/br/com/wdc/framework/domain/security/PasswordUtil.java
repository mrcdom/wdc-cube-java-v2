package br.com.wdc.framework.domain.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * Hashing utilities for authentication.
 * <p>
 * Centralizes the password hashing logic (MD5→base36) and the HMAC-SHA256
 * used in the challenge-response flow. Can be consumed by any
 * layer (presentation, api-client, persistence).
 * <p>
 * Requires a {@link CryptoProvider} to be registered in
 * {@code CryptoProvider.BEAN} before use.
 */
public final class PasswordUtil {

	private PasswordUtil() {
	}

	/**
	 * Password hash: MD5 → BigInteger → base36.
	 * Produces the same value stored in the database.
	 *
	 * @param plainPassword plain-text password
	 * @return hash in base36 format
	 */
	public static String hashPassword(String plainPassword) {
		byte[] input = plainPassword.getBytes(StandardCharsets.UTF_8);
		var provider = CryptoProvider.BEAN.get();
		if (provider == null) {
			throw new IllegalStateException("CryptoProvider.BEAN not initialized");
		}
		byte[] digest = provider.md5(input);
		return new BigInteger(digest).toString(36);
	}

	/**
	 * HMAC-SHA256 for the challenge-response flow.
	 *
	 * @param key  key (passwordHash)
	 * @param data data (userName + nonce)
	 * @return hex-encoded HMAC
	 */
	public static String computeHmac(String key, String data) {
		byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
		byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
		var provider = CryptoProvider.BEAN.get();
		if (provider == null) {
			throw new IllegalStateException("CryptoProvider.BEAN not initialized");
		}
		byte[] hash = provider.hmacSha256(keyBytes, dataBytes);
		return bytesToHex(hash);
	}

	private static String bytesToHex(byte[] bytes) {
		var sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}

}
