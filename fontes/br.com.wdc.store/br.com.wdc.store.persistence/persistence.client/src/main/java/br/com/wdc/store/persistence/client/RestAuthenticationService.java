package br.com.wdc.store.persistence.client;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.SecurityContext;

/**
 * {@link AuthenticationService} implementation over HTTP REST.
 * <p>
 * Wraps the calls to the {@code /api/auth/*} endpoints and manages
 * the access token automatically, so the REST repositories
 * include the Bearer token in subsequent requests.
 * <p>
 * Persists tokens in the {@link ClientStorage} (if available) so the session
 * can be restored after the application restarts.
 */
public class RestAuthenticationService implements AuthenticationService {

	private static final String KEY_ACCESS_TOKEN = "auth.accessToken";
	private static final String KEY_REFRESH_TOKEN = "auth.refreshToken";

	private final HttpTransport transport;
	private final RestAuthClient authClient;
	private final ClientStorage storage;

	public RestAuthenticationService(HttpTransport transport, ClientStorage storage) {
		this.transport = transport;
		this.storage = storage;
		this.authClient = new RestAuthClient(transport);
		transport.setAccessTokenSupplier(authClient::accessToken);
	}

	/**
	 * Tries to restore the session from the tokens saved in the {@link ClientStorage}.
	 * If the refresh token is present, refreshes and returns the result.
	 *
	 * @return the refresh result, or {@code null} if there is no saved session or the refresh fails
	 */
	public AuthResult tryRestore() {
		var savedRefreshToken = storage.secure().get(KEY_REFRESH_TOKEN);
		if (savedRefreshToken == null) {
			return null;
		}

		var result = refresh(savedRefreshToken);
		if (result == null) {
			storage.secure().remove(KEY_ACCESS_TOKEN);
			storage.secure().remove(KEY_REFRESH_TOKEN);
		}
		return result;
	}

	@Override
	public ChallengeResult challenge(String userName) {
		return AuthJson.readChallenge(transport.postJsonPublic("/api/auth/challenge", AuthJson.challengeRequest(userName)));
	}

	@Override
	public AuthResult login(String userName, String proof, String nonce) {
		String responseJson;
		try {
			responseJson = transport.postJsonPublic("/api/auth/login", AuthJson.loginRequest(userName, proof, nonce));
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = AuthJson.readAuthResult(responseJson);

		// Updates the RestAuthClient so the REST repos include the Bearer token
		authClient.setTokens(
				result.accessToken(),
				result.refreshToken(),
				result.publicKey(),
				result.expiresAt().getEpochSecond());

		// Persists tokens in secure storage (AES-GCM) for later restoration
		storage.secure().set(KEY_ACCESS_TOKEN, result.accessToken());
		storage.secure().set(KEY_REFRESH_TOKEN, result.refreshToken());

		return result;
	}

	@Override
	public AuthResult refresh(String refreshToken) {
		if (refreshToken == null) {
			return null;
		}

		String responseJson;
		try {
			responseJson = transport.postJsonPublic("/api/auth/refresh", AuthJson.refreshRequest(refreshToken));
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = AuthJson.readAuthResult(responseJson);

		authClient.setTokens(
				result.accessToken(),
				result.refreshToken(),
				result.publicKey(),
				result.expiresAt().getEpochSecond());

		// Updates tokens in secure storage (AES-GCM)
		storage.secure().set(KEY_ACCESS_TOKEN, result.accessToken());
		storage.secure().set(KEY_REFRESH_TOKEN, result.refreshToken());

		return result;
	}

	@Override
	public void logout(String refreshToken) {
		if (refreshToken != null) {
			try {
				transport.postJsonPublic("/api/auth/logout", AuthJson.refreshRequest(refreshToken));
			} catch (Exception ignored) {
				// Ignores network errors on logout
			}
		}
		authClient.clearTokens();

		// Clears tokens from storage — unconditionally, regardless of the refreshToken
		storage.secure().remove(KEY_ACCESS_TOKEN);
		storage.secure().remove(KEY_REFRESH_TOKEN);
	}

	@Override
	public SecurityContext resolveToken(String jwtToken) {
		// REST clients do not resolve JWT locally — SecurityContext is managed
		// server-side. Return null so callers handle the absent context gracefully.
		return null;
	}

	@Override
	public String createPersistentToken(Long userId, String userName) {
		// For REST clients the persistent token IS the refresh token, which was
		// already stored in storage.secure() during login(). Return it directly.
		return storage.secure().get(KEY_REFRESH_TOKEN);
	}

	@Override
	public AuthResult loginWithPersistentToken(String persistentToken) {
		// For REST clients the "persistent token" stored by LoginPresenter is the
		// refresh token. Delegate to refresh() to obtain a fresh access token.
		var result = refresh(persistentToken);
		// refresh() already saves the NEW refresh token under KEY_REFRESH_TOKEN.
		// Also update the persistent-token entry so the next auto-login works
		// (servers rotate refresh tokens on every use).
		if (result != null) {
			storage.secure().set("auth.token", storage.secure().get(KEY_REFRESH_TOKEN));
		}
		return result;
	}
}
