package br.com.wdc.shopping.view.teavm.repo;

import java.time.Instant;

import com.google.gson.JsonObject;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.shopping.api.client.HttpTransport;
import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.security.AuthResult;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.ChallengeResult;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Implementação de {@link AuthenticationService} para TeaVM. Usa HttpTransport diretamente sem Gson reflection.
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
        var json = transport.getJson("/api/auth/challenge");
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
            response = transport.postJsonPublic("/api/auth/login", body);
        } catch (BusinessException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return null;
            }
            throw e;
        }

        var result = parseAuthResult(response);

        // Guarda access token para requisições subsequentes
        this.accessToken = result.accessToken();
        transport.setAccessTokenSupplier(() -> this.accessToken);

        return result;
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        if (refreshToken == null)
            return null;

        var body = new JsonObject();
        body.addProperty("refreshToken", refreshToken);

        JsonObject response;
        try {
            response = transport.postJsonPublic("/api/auth/refresh", body);
        } catch (BusinessException e) {
            if (e.getMessage() != null && e.getMessage().contains("401")) {
                return null;
            }
            throw e;
        }

        var result = parseAuthResult(response);
        this.accessToken = result.accessToken();
        return result;
    }

    @Override
    public void logout(String refreshToken) {
        if (refreshToken == null)
            return;
        try {
            var body = new JsonObject();
            body.addProperty("refreshToken", refreshToken);
            transport.postJsonPublic("/api/auth/logout", body);
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

    private static AuthResult parseAuthResult(JsonObject json) {
        return new AuthResult(
                json.get("userId").getAsLong(),
                json.get("accessToken").getAsString(),
                json.get("refreshToken").getAsString(),
                Instant.parse(json.get("expiresAt").getAsString()),
                json.get("publicKey").getAsString());
    }

}
