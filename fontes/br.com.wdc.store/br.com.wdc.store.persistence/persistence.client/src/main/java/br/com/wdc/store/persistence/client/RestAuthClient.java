package br.com.wdc.store.persistence.client;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.domain.security.Scram;

/**
 * REST client for authentication through the API.
 * <p>
 * Signs in with SCRAM-SHA-256 and manages the session's tokens.
 */
public class RestAuthClient {

	private static final Scram SCRAM = new Scram(new JceCryptoProvider());

	private final HttpTransport transport;

	private String accessToken;
	private String refreshToken;
	private String publicKeyBase64;
	private long expiresAtEpochSecond;

	public RestAuthClient(HttpTransport transport) {
		this.transport = transport;
	}

	/**
	 * Signs in: the SCRAM-SHA-256 exchange ({@link Scram}), checking that the server holds the user's verifier.
	 *
	 * @param userName user name
	 * @param password the password as the user typed it; it does not leave this process
	 */
	public void login(String userName, String password) {
		var challenge = AuthJson.readChallenge(transport.postJsonPublic("/api/auth/challenge", AuthJson.challengeRequest(userName)));
		var response = SCRAM.respond(userName, password, challenge);
		var result = AuthJson.readAuthResult(transport.postJsonPublic("/api/auth/login",
				AuthJson.loginRequest(userName, response.proof(), challenge.nonce())));
		if (!response.serverIsAuthentic(result.serverSignature())) {
			throw new BusinessException("The server could not prove it holds the user's credentials");
		}
		setTokens(result.accessToken(), result.refreshToken(), result.publicKey(), result.expiresAt().getEpochSecond());
	}

	/**
	 * Renews the access token using the refresh token.
	 */
	public void refresh() {
		if (refreshToken == null) {
			throw new BusinessException("No refresh token available — login first");
		}

		var result = AuthJson.readAuthResult(transport.postJsonPublic("/api/auth/refresh", AuthJson.refreshRequest(refreshToken)));
		setTokens(result.accessToken(), result.refreshToken(), result.publicKey(), result.expiresAt().getEpochSecond());
	}

	/**
	 * Logout — ends the session on the server.
	 */
	public void logout() {
		if (accessToken != null) {
			try {
				var writer = new JsonStreamWriter();
				writer.beginObject();
				writer.endObject();
				transport.postJsonWithAuth("/api/auth/logout", writer.result(), accessToken);
			} catch (Exception e) {
				// Ignores network errors on logout
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
	 * Sets the tokens directly (used by {@link RestAuthenticationService}).
	 */
	void setTokens(String accessToken, String refreshToken, String publicKeyBase64, long expiresAtEpochSecond) {
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
		this.publicKeyBase64 = publicKeyBase64;
		this.expiresAtEpochSecond = expiresAtEpochSecond;
	}

	/**
	 * Clears all tokens (used by {@link RestAuthenticationService} on logout).
	 */
	void clearTokens() {
		this.accessToken = null;
		this.refreshToken = null;
		this.publicKeyBase64 = null;
		this.expiresAtEpochSecond = 0;
	}

	/**
	 * Encrypts a password with the session's RSA public key.
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
}
