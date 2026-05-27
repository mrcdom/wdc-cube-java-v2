package br.com.wdc.shopping.view.teavm.repo;

import java.time.Instant;

import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.persistence.client.HttpTransport;
import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.security.AuthResult;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.ChallengeResult;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Implementação de {@link AuthenticationService} para TeaVM. Usa HttpTransport diretamente.
 */
public class TeaVMAuthenticationService implements AuthenticationService {

    private final Log LOG = Log.getLogger(TeaVMAuthenticationService.class.getSimpleName());

    private final HttpTransport transport;
    private String accessToken;

    public TeaVMAuthenticationService(HttpTransport transport) {
        this.transport = transport;
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

        // Guarda access token para requisições subsequentes
        this.accessToken = result.accessToken();
        transport.setAccessTokenSupplier(() -> this.accessToken);

        return result;
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null)
            return null;

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
        this.accessToken = result.accessToken();
        return result;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null)
            return;
        try {
            var writer = new JsonStreamWriter();
            writer.beginObject();
            writer.name("refreshToken").value(refreshToken);
            writer.endObject();
            transport.postJsonPublic("/api/auth/logout", writer.result());
        } catch (Exception e) {
            LOG.debug("logout: " + e.getMessage());
        }
        this.accessToken = null;
        transport.setAccessTokenSupplier(null);
    }

    @Override
    public SecurityContext resolveToken(String jwtToken) {
        // Client-side: não resolve token localmente
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
