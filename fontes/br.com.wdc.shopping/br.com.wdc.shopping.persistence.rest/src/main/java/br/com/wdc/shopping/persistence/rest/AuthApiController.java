package br.com.wdc.shopping.persistence.rest;

import org.apache.commons.lang3.StringUtils;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.persistence.rest.doc.Doc;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Adaptador HTTP para o {@link AuthenticationService}.
 * <p>
 * Endpoints:
 * <ul>
 * <li>{@code GET /api/auth/challenge} — gera nonce para login</li>
 * <li>{@code POST /api/auth/login} — autentica via HMAC challenge-response</li>
 * <li>{@code POST /api/auth/refresh} — renova access token</li>
 * <li>{@code POST /api/auth/logout} — encerra sessão</li>
 * </ul>
 */
public class AuthApiController {

    public static void configure(JavalinConfig config, String prefix) {
        var ctrl = new AuthApiController();
        config.routes.get(chalengePath(prefix), ctrl::challenge);
        config.routes.post(loginPath(prefix), ctrl::login);
        config.routes.post(refreshPath(prefix), ctrl::refresh);
        config.routes.post(logoutPath(prefix), ctrl::logout);
    }

    public static void openApi(OpenAPI api, String prefix) {
        var ctrl = new AuthApiController();
        ctrl.challengeDoc(api, prefix);
        ctrl.loginDoc(api, prefix);
        ctrl.refreshDoc(api, prefix);
        ctrl.logoutDoc(api, prefix);
    }

    // :: Challange

    private static String chalengePath(String prefix) {
        return prefix + "/api/auth/challenge";
    }

    private void challengeDoc(OpenAPI api, String prefix) {
        var operation = new Operation()
                .addTagsItem("auth").summary("Request an HMAC challenge nonce")
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/ChallengeResponse")));

        api.path(chalengePath(prefix), new PathItem().get(operation));
    }

    private void challenge(Context ctx) {
        var authService = AuthenticationService.BEAN.get();
        var result = authService.challenge();
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("nonce").value(result.nonce());
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
                .addTagsItem("auth").summary("Authenticate with HMAC challenge-response")
                .requestBody(Doc.body("#/components/schemas/LoginRequest"))
                .responses(new ApiResponses()
                        .addApiResponse("200", Doc.ok("#/components/schemas/TokenResponse"))
                        .addApiResponse("401", Doc.unauthorized()));

        api.path(loginPath(prefix), new PathItem().post(operation));
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

        var authService = AuthenticationService.BEAN.get();
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
                        .addApiResponse("401", Doc.unauthorized()));

        api.path(refreshPath(prefix), new PathItem().post(operation));
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

        var authService = AuthenticationService.BEAN.get();
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

    private static void json(Context ctx, JsonStreamWriter writer) {
        ctx.contentType("application/json");
        ctx.result(writer.result());
    }

}