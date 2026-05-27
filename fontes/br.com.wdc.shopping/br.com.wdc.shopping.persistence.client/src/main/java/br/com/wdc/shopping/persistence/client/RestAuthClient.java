package br.com.wdc.shopping.persistence.client;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.exception.BusinessException;

/**
 * Cliente REST para autenticação via API.
 * <p>
 * Implementa o fluxo challenge-response (HMAC) e gerencia tokens.
 */
public class RestAuthClient {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final RestConfig config;

	private String accessToken;
	private String refreshToken;
	private String publicKeyBase64;
	private long expiresAtEpochSecond;

	public RestAuthClient(RestConfig config) {
		this.config = config;
	}

	/**
	 * Realiza o login completo: challenge + HMAC + token.
	 *
	 * @param userName     nome do usuário
	 * @param passwordHash hash MD5-base36 da senha (como armazenado no banco)
	 */
	public void login(String userName, String passwordHash) {
		// 1. Obter challenge (nonce)
		var challengeJson = config.getJson("/api/auth/challenge");
		var challengeReader = new JsonStreamReader(challengeJson);
		challengeReader.beginObject();
		String nonce = null;
		while (challengeReader.hasNext()) {
			switch (challengeReader.nextName()) {
				case "nonce" -> nonce = InputCoerceUtils.asString(challengeReader);
				default -> challengeReader.skipValue();
			}
		}
		challengeReader.endObject();

		// 2. Calcular HMAC: key=passwordHash, data=userName+nonce
		var digest = computeHmac(passwordHash, userName + nonce);

		// 3. Enviar login
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("userName").value(userName);
		writer.name("digest").value(digest);
		writer.name("nonce").value(nonce);
		writer.endObject();

		var loginJson = config.postJsonPublic("/api/auth/login", writer.result());
		var loginReader = new JsonStreamReader(loginJson);
		loginReader.beginObject();
		while (loginReader.hasNext()) {
			switch (loginReader.nextName()) {
				case "accessToken" -> this.accessToken = InputCoerceUtils.asString(loginReader);
				case "refreshToken" -> this.refreshToken = InputCoerceUtils.asString(loginReader);
				case "publicKey" -> this.publicKeyBase64 = InputCoerceUtils.asString(loginReader);
				case "expiresAt" -> {
					var s = InputCoerceUtils.asString(loginReader);
					if (s != null) this.expiresAtEpochSecond = java.time.Instant.parse(s).getEpochSecond();
				}
				default -> loginReader.skipValue();
			}
		}
		loginReader.endObject();
	}

	/**
	 * Renova o access token usando o refresh token.
	 */
	public void refresh() {
		if (refreshToken == null) {
			throw new BusinessException("No refresh token available — login first");
		}

		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("refreshToken").value(refreshToken);
		writer.endObject();

		var responseJson = config.postJsonPublic("/api/auth/refresh", writer.result());
		var reader = new JsonStreamReader(responseJson);
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "accessToken" -> this.accessToken = InputCoerceUtils.asString(reader);
				case "refreshToken" -> this.refreshToken = InputCoerceUtils.asString(reader);
				case "publicKey" -> this.publicKeyBase64 = InputCoerceUtils.asString(reader);
				case "expiresAt" -> {
					var s = InputCoerceUtils.asString(reader);
					if (s != null) this.expiresAtEpochSecond = java.time.Instant.parse(s).getEpochSecond();
				}
				default -> reader.skipValue();
			}
		}
		reader.endObject();
	}

	/**
	 * Logout — encerra a sessão no servidor.
	 */
	public void logout() {
		if (accessToken != null) {
			try {
				var writer = new JsonStreamWriter();
				writer.beginObject();
				writer.endObject();
				config.postJsonWithAuth("/api/auth/logout", writer.result(), accessToken);
			} catch (Exception e) {
				// Ignora erros de rede no logout
			}
		}
		this.accessToken = null;
		this.refreshToken = null;
		this.publicKeyBase64 = null;
		this.expiresAtEpochSecond = 0;
	}

	public String accessToken() {
		return accessToken;
	}

	public String publicKeyBase64() {
		return publicKeyBase64;
	}

	public boolean isAuthenticated() {
		return accessToken != null;
	}
	
	public long getExpiresAtEpochSecond() {
        return expiresAtEpochSecond;
    }

	/**
	 * Define os tokens diretamente (usado por {@link RestAuthenticationService}).
	 */
	void setTokens(String accessToken, String refreshToken, String publicKeyBase64, long expiresAtEpochSecond) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.publicKeyBase64 = publicKeyBase64;
		this.expiresAtEpochSecond = expiresAtEpochSecond;
	}

	/**
	 * Limpa todos os tokens (usado por {@link RestAuthenticationService} no logout).
	 */
	void clearTokens() {
		this.accessToken = null;
		this.refreshToken = null;
		this.publicKeyBase64 = null;
		this.expiresAtEpochSecond = 0;
	}

	/**
	 * Criptografa uma senha com a chave pública RSA da sessão.
	 */
	public String encryptPassword(String plainPassword) {
		if (publicKeyBase64 == null) {
			throw new BusinessException("No public key available — login first");
		}
		try {
			byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
			PublicKey publicKey = KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(keyBytes));
			Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, publicKey);
			byte[] encrypted = cipher.doFinal(plainPassword.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			throw BusinessException.wrap("RSA encryption failed", e);
		}
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
