package br.com.wdc.shopping.domain.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

/**
 * Utilitários de hashing para autenticação.
 * <p>
 * Centraliza a lógica de hash de senha (MD5→base36) e HMAC-SHA256
 * usados no fluxo challenge-response. Pode ser consumido por qualquer
 * camada (presentation, api-client, persistence).
 * <p>
 * Requer que um {@link CryptoProvider} esteja registrado em
 * {@code CryptoProvider.BEAN} antes do uso.
 */
public final class PasswordUtil {

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
		byte[] input = plainPassword.getBytes(StandardCharsets.UTF_8);
		var provider = CryptoProvider.BEAN.get();
		if (provider == null) {
			throw new IllegalStateException("CryptoProvider.BEAN not initialized");
		}
		byte[] digest = provider.md5(input);
		return new BigInteger(digest).toString(36);
	}

	/**
	 * HMAC-SHA256 para o fluxo challenge-response.
	 *
	 * @param key  chave (passwordHash)
	 * @param data dados (userName + nonce)
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
