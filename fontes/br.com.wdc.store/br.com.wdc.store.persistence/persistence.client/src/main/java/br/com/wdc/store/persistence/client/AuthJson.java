package br.com.wdc.store.persistence.client;

import java.time.Instant;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.domain.security.AuthenticationService.AuthResult;
import br.com.wdc.framework.domain.security.AuthenticationService.ChallengeResult;

/** The bodies of {@code /api/auth}, for every client of it (the JVM's and the browser's). */
public final class AuthJson {

    private AuthJson() {
        // NOOP
    }

    /** The body of {@code POST /api/auth/challenge}. */
    public static String challengeRequest(String userName) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("userName").value(userName);
        writer.endObject();
        return writer.result();
    }

    public static ChallengeResult readChallenge(String json) {
        var reader = new JsonStreamReader(json);
        reader.beginObject();
        String nonce = null;
        String salt = null;
        int iterations = 0;
        Instant expiresAt = null;
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "nonce" -> nonce = InputCoerceUtils.asString(reader);
                case "salt" -> salt = InputCoerceUtils.asString(reader);
                case "iterations" -> {
                    var n = InputCoerceUtils.asInteger(reader);
                    iterations = n != null ? n : 0;
                }
                case "expiresAt" -> {
                    var s = InputCoerceUtils.asString(reader);
                    if (s != null) expiresAt = Instant.parse(s);
                }
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new ChallengeResult(nonce, salt, iterations, expiresAt);
    }

    /** The body of {@code POST /api/auth/login}. */
    public static String loginRequest(String userName, String proof, String nonce) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("userName").value(userName);
        writer.name("proof").value(proof);
        writer.name("nonce").value(nonce);
        writer.endObject();
        return writer.result();
    }

    /** The body of {@code POST /api/auth/refresh} and {@code /logout}. */
    public static String refreshRequest(String refreshToken) {
        var writer = new JsonStreamWriter();
        writer.beginObject();
        writer.name("refreshToken").value(refreshToken);
        writer.endObject();
        return writer.result();
    }

    /** The answer of a login or a refresh. */
    public static AuthResult readAuthResult(String json) {
        var reader = new JsonStreamReader(json);
        reader.beginObject();
        Long userId = null;
        String accessToken = null;
        String refreshToken = null;
        Instant expiresAt = null;
        String publicKey = null;
        String serverSignature = null;
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
                case "serverSignature" -> serverSignature = InputCoerceUtils.asString(reader);
                default -> reader.skipValue();
            }
        }
        reader.endObject();
        return new AuthResult(userId, accessToken, refreshToken, expiresAt, publicKey, serverSignature);
    }
}
