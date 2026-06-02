package br.com.wdc.shopping.persistence.client;

import java.time.Instant;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.security.AuthResult;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.ChallengeResult;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Implementação de {@link AuthenticationService} sobre HTTP REST.
 * <p>
 * Encapsula as chamadas aos endpoints {@code /api/auth/*} e gerencia
 * o token de acesso automaticamente, permitindo que os repositórios REST
 * incluam o Bearer token em requisições subsequentes.
 * <p>
 * Persiste tokens no {@link ClientStorage} (se disponível) para que a sessão
 * possa ser restaurada após reinicialização da aplicação.
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
	 * Tenta restaurar a sessão a partir dos tokens salvos no {@link ClientStorage}.
	 * Se o refresh token estiver presente, faz refresh e retorna o resultado.
	 *
	 * @return resultado do refresh, ou {@code null} se não houver sessão salva ou o refresh falhar
	 */
	public AuthResult tryRestore() {
		var savedRefreshToken = storage.get(KEY_REFRESH_TOKEN);
		if (savedRefreshToken == null) {
			return null;
		}

		var result = refresh(savedRefreshToken);
		if (result == null) {
			storage.remove(KEY_ACCESS_TOKEN);
			storage.remove(KEY_REFRESH_TOKEN);
		}
		return result;
	}

	@Override
	public ChallengeResult challenge() {
		var responseJson = transport.getJson("/api/auth/challenge");
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
			responseJson = transport.postJsonPublic("/api/auth/login", writer.result());
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

		// Persiste tokens para restauração futura
		storage.set(KEY_ACCESS_TOKEN, result.accessToken());
		storage.set(KEY_REFRESH_TOKEN, result.refreshToken());

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
			responseJson = transport.postJsonPublic("/api/auth/refresh", writer.result());
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

		// Atualiza tokens no storage
		storage.set(KEY_ACCESS_TOKEN, result.accessToken());
		storage.set(KEY_REFRESH_TOKEN, result.refreshToken());

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
			transport.postJsonPublic("/api/auth/logout", writer.result());
		} catch (Exception ignored) {
			// Ignora erros de rede no logout
		}
		authClient.clearTokens();

		// Limpa tokens do storage
		storage.remove(KEY_ACCESS_TOKEN);
		storage.remove(KEY_REFRESH_TOKEN);
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
