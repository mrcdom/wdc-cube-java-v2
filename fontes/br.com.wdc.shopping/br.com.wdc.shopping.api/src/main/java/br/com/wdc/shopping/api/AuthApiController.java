package br.com.wdc.shopping.api;

import java.util.Map;

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
		config.routes.get("/api/auth/challenge", this::challenge);
		config.routes.post("/api/auth/login", this::login);
		config.routes.post("/api/auth/refresh", this::refresh);
		config.routes.post("/api/auth/logout", this::logout);
	}

	private void challenge(Context ctx) throws Exception {
		var result = authService.challenge();
		json(ctx, Map.of(
				"nonce", result.nonce(),
				"expiresAt", result.expiresAt().toString()));
	}

	private void login(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());

		var userName = body.has("userName") ? body.get("userName").asText() : null;
		var digest = body.has("digest") ? body.get("digest").asText() : null;
		var nonce = body.has("nonce") ? body.get("nonce").asText() : null;

		if (userName == null || digest == null || nonce == null) {
			ctx.status(400).json(Map.of("error", "Missing required fields: userName, digest, nonce"));
			return;
		}

		var result = authService.login(userName, digest, nonce);
		if (result == null) {
			ctx.status(401).json(Map.of("error", "Invalid credentials"));
			return;
		}

		json(ctx, Map.of(
				"userId", result.userId(),
				"accessToken", result.accessToken(),
				"refreshToken", result.refreshToken(),
				"expiresAt", result.expiresAt().toString(),
				"publicKey", result.publicKey()));
	}

	private void refresh(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());

		var refreshToken = body.has("refreshToken") ? body.get("refreshToken").asText() : null;
		if (refreshToken == null) {
			ctx.status(400).json(Map.of("error", "Missing refreshToken"));
			return;
		}

		var result = authService.refresh(refreshToken);
		if (result == null) {
			ctx.status(401).json(Map.of("error", "Invalid or expired refresh token"));
			return;
		}

		json(ctx, Map.of(
				"userId", result.userId(),
				"accessToken", result.accessToken(),
				"refreshToken", result.refreshToken(),
				"expiresAt", result.expiresAt().toString(),
				"publicKey", result.publicKey()));
	}

	private void logout(Context ctx) throws Exception {
		var mapper = ApiObjectMapper.get();
		var body = mapper.readTree(ctx.body());

		var refreshToken = body.has("refreshToken") ? body.get("refreshToken").asText() : null;
		authService.logout(refreshToken);
		json(ctx, Map.of("success", true));
	}

	private static void json(Context ctx, Object obj) throws Exception {
		ctx.contentType("application/json");
		ctx.result(ApiObjectMapper.get().writeValueAsString(obj));
	}
}
