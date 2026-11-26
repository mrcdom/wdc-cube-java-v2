package br.com.wdc.store.persistence.rest;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.store.domain.StoreConfig;
import br.com.wdc.store.persistence.rest.doc.Doc;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * HTTP adapter for the {@link AuthenticationService}: the SCRAM-SHA-256 sign-in and the session's tokens.
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code POST /api/auth/challenge} — starts a user's sign-in: a nonce and the parameters of the user's verifier</li>
 * <li>{@code POST /api/auth/login} — ends it with the client's SCRAM proof; the answer carries the server's
 * signature</li>
 * <li>{@code POST /api/auth/refresh} — renews the access token</li>
 * <li>{@code POST /api/auth/logout} — ends the session</li>
 * </ul>
 * Challenge, login and refresh count against the per-address limit ({@link AuthRateLimit}); too many failed sign-ins
 * for a user answer 429 too, with {@code Retry-After}.
 */
public class AuthApiController {

    private final AuthRateLimit limit;

    private AuthApiController(AuthRateLimit limit) {
        this.limit = limit;
    }

    public static void configure(JavalinConfig config, String prefix) {
        var ctrl = new AuthApiController(new AuthRateLimit(StoreConfig.getAuthRequestsPerMinute()));
        config.routes.post(challengePath(prefix), ctrl::challenge);
        config.routes.post(loginPath(prefix), ctrl::login);
        config.routes.post(refreshPath(prefix), ctrl::refresh);
        config.routes.post(logoutPath(prefix), ctrl::logout);
    }

    public static void openApi(OpenAPI api, String prefix) {
        var ctrl = new AuthApiController(null);
        ctrl.challengeDoc(api, prefix);
        ctrl.loginDoc(api, prefix);
        ctrl.refreshDoc(api, prefix);
        ctrl.logoutDoc(api, prefix);
    }

    // :: Challenge

    private static String challengePath(String prefix) {
        return prefix + "/api/auth/challenge";
    }

    private void challengeDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("auth").summary("Start a user's SCRAM-SHA-256 sign-in: a nonce, the salt and the iterations")
                .requestBody(Doc.body("#/components/schemas/ChallengeRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/ChallengeResponse"))
                        .addApiResponse("429", Doc.tooManyRequests()));

        api.path(challengePath(prefix), new PathItem().post(operation));
    }

    private void challenge(Context ctx) {
        limit.check(ctx.ip());
        var reader = new JsonStreamReader(ctx.body());
        String userName = null;
        reader.beginObject();
        while (reader.hasNext()) {
            if ("userName".equals(reader.nextName())) {
                userName = InputCoerceUtils.asString(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        if (StringUtils.isBlank(userName)) {
            ctx.status(400).result("{\"error\":\"Missing required field: userName\"}");
            return;
        }

        var result = AuthenticationService.BEAN.get().challenge(userName);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("nonce").value(result.nonce());
        writer.name("salt").value(result.salt());
        writer.name("iterations").value(result.iterations());
        writer.name("expiresAt").value(result.expiresAt().toString());
        writer.endObject();
        json(ctx, writer);
    }

    // :: login

    private static String loginPath(String prefix) {
        return prefix + "/api/auth/login";
    }

    private void loginDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("auth").summary("End the sign-in with the client's SCRAM proof")
                .requestBody(Doc.body("#/components/schemas/LoginRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/TokenResponse"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("429", Doc.tooManyRequests()));

        api.path(loginPath(prefix), new PathItem().post(operation));
    }

    @SuppressWarnings("java:S2589") // false positive — variables are assigned inside switch-in-while
    private void login(Context ctx) {
        limit.check(ctx.ip());
        var reader = new JsonStreamReader(ctx.body());
        String userName = null;
        String proof = null;
        String nonce = null;
        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
            case "userName" -> userName = InputCoerceUtils.asString(reader);
            case "proof" -> proof = InputCoerceUtils.asString(reader);
            case "nonce" -> nonce = InputCoerceUtils.asString(reader);
            default -> reader.skipValue();
            }
        }
        reader.endObject();

        if (StringUtils.isBlank(userName) || StringUtils.isBlank(proof) || StringUtils.isBlank(nonce)) {
            ctx.status(400).result("{\"error\":\"Missing required fields: userName, proof, nonce\"}");
            return;
        }

        var result = AuthenticationService.BEAN.get().login(userName, proof, nonce);
        if (result == null) {
            ctx.status(401).result("{\"error\":\"Invalid credentials\"}");
            return;
        }

        writeTokens(ctx, result);
    }

    // :: Refresh

    private static String refreshPath(String prefix) {
        return prefix + "/api/auth/refresh";
    }

    private void refreshDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("auth").summary("Refresh the access token using a refresh token")
                .requestBody(Doc.body("#/components/schemas/RefreshRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/TokenResponse"))
                        .addApiResponse("401", Doc.unauthorized())
                        .addApiResponse("429", Doc.tooManyRequests()));

        api.path(refreshPath(prefix), new PathItem().post(operation));
    }

    private void refresh(Context ctx) {
        limit.check(ctx.ip());
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

        var authService = AuthenticationService.BEAN.get();
        var result = authService.refresh(refreshToken);
        if (result == null) {
            ctx.status(401).result("{\"error\":\"Invalid or expired refresh token\"}");
            return;
        }

        writeTokens(ctx, result);
    }

    // :: Logout

    private static String logoutPath(String prefix) {
        return prefix + "/api/auth/logout";
    }

    private void logoutDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("auth").summary("Revoke the session (invalidate the refresh token)")
                .requestBody(Doc.body("#/components/schemas/RefreshRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/MutationResult"))
                        .addApiResponse("401", Doc.unauthorized()));

        api.path(logoutPath(prefix), new PathItem().post(operation));
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

        var authService = AuthenticationService.BEAN.get();
        authService.logout(refreshToken);
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("success").value(true);
        writer.endObject();
        json(ctx, writer);
    }

    /** The session's tokens; after a sign-in, with the server's SCRAM signature. */
    private static void writeTokens(Context ctx, AuthenticationService.AuthResult result) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("userId").value(result.userId());
        writer.name("accessToken").value(result.accessToken());
        writer.name("refreshToken").value(result.refreshToken());
        writer.name("expiresAt").value(result.expiresAt().toString());
        writer.name("publicKey").value(result.publicKey());
        if (result.serverSignature() != null) {
            writer.name("serverSignature").value(result.serverSignature());
        }
        writer.endObject();
        json(ctx, writer);
    }

    private static void json(Context ctx, JsonStreamWriter writer) {
        ctx.contentType("application/json");
        ctx.result(writer.result());
    }

}