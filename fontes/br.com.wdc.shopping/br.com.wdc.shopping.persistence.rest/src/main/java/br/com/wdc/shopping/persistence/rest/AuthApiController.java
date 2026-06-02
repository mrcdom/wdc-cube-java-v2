package br.com.wdc.shopping.persistence.rest;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Adaptador HTTP para o {@link AuthenticationService}.
 * <p>
 * Endpoints:
 * <ul>
 *   <li>{@code GET /api/auth/challenge} — gera nonce para login</li>
 *   <li>{@code POST /api/auth/login} — autentica via HMAC challenge-response</li>
 *   <li>{@code POST /api/auth/refresh} — renova access token</li>
 *   <li>{@code POST /api/auth/logout} — encerra sessão</li>
 * </ul>
 */
public class AuthApiController {

	private final AuthenticationService authService;

	public AuthApiController(AuthenticationService authService) {
		this.authService = authService;
	}

	public void configure(JavalinConfig config) {
		configure(config, "");
	}

	public void configure(JavalinConfig config, String prefix) {
		config.routes.get(prefix + "/api/auth/challenge", this::challenge);
		config.routes.post(prefix + "/api/auth/login", this::login);
		config.routes.post(prefix + "/api/auth/refresh", this::refresh);
		config.routes.post(prefix + "/api/auth/logout", this::logout);
	}

	private void challenge(Context ctx) {
		var result = authService.challenge();
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("nonce").value(result.nonce());
		writer.name("expiresAt").value(result.expiresAt().toString());
		writer.endObject();
		json(ctx, writer);
	}

	@SuppressWarnings("java:S2589") // false positive — variables are assigned inside switch-in-while
	private void login(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		String userName = null;
		String digest = null;
		String nonce = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "userName" -> userName = InputCoerceUtils.asString(reader);
				case "digest" -> digest = InputCoerceUtils.asString(reader);
				case "nonce" -> nonce = InputCoerceUtils.asString(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();

		if (StringUtils.isBlank(userName) || StringUtils.isBlank(digest) || StringUtils.isBlank(nonce)) {
			ctx.status(400).result("{\"error\":\"Missing required fields: userName, digest, nonce\"}");
			return;
		}

		var result = authService.login(userName, digest, nonce);
		if (result == null) {
			ctx.status(401).result("{\"error\":\"Invalid credentials\"}");
			return;
		}

		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("userId").value(result.userId());
		writer.name("accessToken").value(result.accessToken());
		writer.name("refreshToken").value(result.refreshToken());
		writer.name("expiresAt").value(result.expiresAt().toString());
		writer.name("publicKey").value(result.publicKey());
		writer.endObject();
		json(ctx, writer);
	}

	private void refresh(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		String refreshToken = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "refreshToken" -> refreshToken = InputCoerceUtils.asString(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();

		if (refreshToken == null) {
			ctx.status(400).result("{\"error\":\"Missing refreshToken\"}");
			return;
		}

		var result = authService.refresh(refreshToken);
		if (result == null) {
			ctx.status(401).result("{\"error\":\"Invalid or expired refresh token\"}");
			return;
		}

		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("userId").value(result.userId());
		writer.name("accessToken").value(result.accessToken());
		writer.name("refreshToken").value(result.refreshToken());
		writer.name("expiresAt").value(result.expiresAt().toString());
		writer.name("publicKey").value(result.publicKey());
		writer.endObject();
		json(ctx, writer);
	}

	private void logout(Context ctx) {
		var reader = new JsonStreamReader(ctx.body());
		String refreshToken = null;
		reader.beginObject();
		while (reader.hasNext()) {
			switch (reader.nextName()) {
				case "refreshToken" -> refreshToken = InputCoerceUtils.asString(reader);
				default -> reader.skipValue();
			}
		}
		reader.endObject();

		authService.logout(refreshToken);
		var writer = new JsonStreamWriter();
		writer.beginObject();
		writer.name("success").value(true);
		writer.endObject();
		json(ctx, writer);
	}

	private static void json(Context ctx, JsonStreamWriter writer) {
		ctx.contentType("application/json");
		ctx.result(writer.result());
	}
}
