package br.com.wdc.shopping.persistence.client;

import java.time.Instant;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
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
		var responseJson = config.getJson("/api/auth/challenge");
		var reader = new JsonStreamReader(responseJson);
		reader.beginObject();
		String nonce = null;
		Instant expiresAt = null;
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "nonce" -> nonce = InputCoerceUtils.asString(reader);
				case "expiresAt" -> {
					var s = InputCoerceUtils.asString(reader);
					if (s != null) expiresAt = Instant.parse(s);
				}
				default -> reader.skipValue();
			}
		}
		reader.endObject();
		return new ChallengeResult(nonce, expiresAt);
	}

	@Override
	public AuthResult login(String userName, String digest, String nonce) {
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("userName").value(userName);
		writer.name("digest").value(digest);
		writer.name("nonce").value(nonce);
		writer.endObject();

		String responseJson;
		try {
			responseJson = config.postJsonPublic("/api/auth/login", writer.result());
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = parseAuthResult(responseJson);

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

		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("refreshToken").value(refreshToken);
		writer.endObject();

		String responseJson;
		try {
			responseJson = config.postJsonPublic("/api/auth/refresh", writer.result());
		} catch (BusinessException e) {
			if (e.getMessage() != null && e.getMessage().contains("401")) {
				return null;
			}
			throw e;
		}

		var result = parseAuthResult(responseJson);

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
			var writer = new JsonStreamWriter();
			writer.beginObject();
			writer.name("refreshToken").value(refreshToken);
			writer.endObject();
			config.postJsonPublic("/api/auth/logout", writer.result());
		} catch (Exception ignored) {
			// Ignora erros de rede no logout
		}
		authClient.clearTokens();
	}

	@Override
	public SecurityContext resolveToken(String jwtToken) {
		// Resolução de token é server-side only.
		return null;
	}

	private static AuthResult parseAuthResult(String responseJson) {
		var reader = new JsonStreamReader(responseJson);
		reader.beginObject();
		Long userId = null;
		String accessToken = null;
		String refreshToken = null;
		Instant expiresAt = null;
		String publicKey = null;
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "userId" -> userId = InputCoerceUtils.asLong(reader);
				case "accessToken" -> accessToken = InputCoerceUtils.asString(reader);
				case "refreshToken" -> refreshToken = InputCoerceUtils.asString(reader);
				case "expiresAt" -> {
					var s = InputCoerceUtils.asString(reader);
					if (s != null) expiresAt = Instant.parse(s);
				}
				case "publicKey" -> publicKey = InputCoerceUtils.asString(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();
		return new AuthResult(userId, accessToken, refreshToken, expiresAt, publicKey);
	}
}
