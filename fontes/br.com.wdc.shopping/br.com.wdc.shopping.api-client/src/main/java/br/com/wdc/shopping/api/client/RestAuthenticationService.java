package br.com.wdc.shopping.api.client;

import java.time.Instant;

import com.google.gson.JsonObject;

import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.security.AuthResult;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.ChallengeResult;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Implementação de {@link AuthenticationService} sobre HTTP REST.
 * <p>
 * Encapsula as chamadas aos endpoints {@code /api/auth/*} e gerencia
 * o token de acesso automaticamente no {@link RestAuthClient} vinculado
 * ao {@link RestConfig}, permitindo que os repositórios REST incluam
 * o Bearer token em requisições subsequentes.
 */
public class RestAuthenticationService implements AuthenticationService {

	private final RestConfig config;
	private final RestAuthClient authClient;

	public RestAuthenticationService(RestConfig config) {
		this.config = config;
		this.authClient = new RestAuthClient(config);
		config.setAuthClient(authClient);
	}

	@Override
	public ChallengeResult challenge() {
		var json = config.getJson("/api/auth/challenge");
		var nonce = json.get("nonce").getAsString();
		var expiresAt = Instant.parse(json.get("expiresAt").getAsString());
		return new ChallengeResult(nonce, expiresAt);
	}

	@Override
	public AuthResult login(String userName, String digest, String nonce) {
		var body = new JsonObject();
		body.addProperty("userName", userName);
		body.addProperty("digest", digest);
		body.addProperty("nonce", nonce);

		JsonObject response;
		try {
			response = config.postJsonPublic("/api/auth/login", body);
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = parseAuthResult(response);

		// Atualiza o RestAuthClient para que REST repos incluam o Bearer token
		authClient.setTokens(
				result.accessToken(),
				result.refreshToken(),
				result.publicKey(),
				result.expiresAt().getEpochSecond());

		return result;
	}

	@Override
	public AuthResult refresh(String refreshToken) {
		if (refreshToken == null) {
			return null;
		}

		var body = new JsonObject();
		body.addProperty("refreshToken", refreshToken);

		JsonObject response;
		try {
			response = config.postJsonPublic("/api/auth/refresh", body);
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = parseAuthResult(response);

		authClient.setTokens(
				result.accessToken(),
				result.refreshToken(),
				result.publicKey(),
				result.expiresAt().getEpochSecond());

		return result;
	}

	@Override
	public void logout(String refreshToken) {
		if (refreshToken == null) {
			return;
		}
		try {
			var body = new JsonObject();
			body.addProperty("refreshToken", refreshToken);
			config.postJsonPublic("/api/auth/logout", body);
		} catch (Exception ignored) {
			// Ignora erros de rede no logout
		}
		authClient.clearTokens();
	}

	@Override
	public SecurityContext resolveToken(String jwtToken) {
		// Resolução de token é server-side only.
		// No client REST, o SecurityContextHolder não é usado —
		// os REST repos enviam o Bearer token e o servidor valida.
		return null;
	}

	private static AuthResult parseAuthResult(JsonObject json) {
		return new AuthResult(
				json.get("userId").getAsLong(),
				json.get("accessToken").getAsString(),
				json.get("refreshToken").getAsString(),
				Instant.parse(json.get("expiresAt").getAsString()),
				json.get("publicKey").getAsString());
	}

}
