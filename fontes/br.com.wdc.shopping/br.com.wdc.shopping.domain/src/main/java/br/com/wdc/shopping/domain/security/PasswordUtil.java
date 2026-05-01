package br.com.wdc.shopping.domain.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Utilitários de hashing para autenticação.
 * <p>
 * Centraliza a lógica de hash de senha (MD5→base36) e HMAC-SHA256
 * usados no fluxo challenge-response. Pode ser consumido por qualquer
 * camada (presentation, api-client, persistence).
 */
public final class PasswordUtil {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private PasswordUtil() {
	}

	/**
	 * Hash de senha: MD5 → BigInteger → base36.
	 * Produz o mesmo valor armazenado no banco de dados.
	 *
	 * @param plainPassword senha em texto plano
	 * @return hash no formato base36
	 */
	public static String hashPassword(String plainPassword) {
		try {
			var md = MessageDigest.getInstance("MD5");
			var digest = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
			return new BigInteger(digest).toString(36);
		} catch (NoSuchAlgorithmException e) {
			throw new AssertionError("MD5 not available", e);
		}
	}

	/**
	 * HMAC-SHA256 para o fluxo challenge-response.
	 *
	 * @param key  chave (passwordHash)
	 * @param data dados (userName + nonce)
	 * @return hex-encoded HMAC
	 */
	public static String computeHmac(String key, String data) {
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
