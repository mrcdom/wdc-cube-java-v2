package br.com.wdc.shopping.view.teavm.repo;

import java.time.Instant;

import br.com.wdc.framework.commons.http.HttpTransport;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.InputCoerceUtils;
import br.com.wdc.framework.commons.serialization.JsonStreamReader;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.shopping.domain.exception.BusinessException;
import br.com.wdc.shopping.domain.security.AuthenticationService;
import br.com.wdc.shopping.domain.security.SecurityContext;

/**
 * Implementação de {@link AuthenticationService} para TeaVM. Usa HttpTransport diretamente.
 * <p>
 * Persiste tokens no {@link ClientStorage} para que a sessão sobreviva ao reload (F5).
 */
public class TeaVMAuthenticationService implements AuthenticationService {

    private static final String KEY_ACCESS_TOKEN = "auth.accessToken";
    private static final String KEY_REFRESH_TOKEN = "auth.refreshToken";

    private final Log log = Log.getLogger(TeaVMAuthenticationService.class.getSimpleName());

    private final HttpTransport transport;
    private final ClientStorage storage;
    private String accessToken;

    public TeaVMAuthenticationService(HttpTransport transport, ClientStorage storage) {
        this.transport = transport;
        this.storage = storage;
    }

    /**
     * Tenta restaurar a sessão a partir dos tokens salvos no {@link ClientStorage}.
     * Se o refresh token estiver presente, faz refresh e retorna o resultado.
     *
     * @return resultado do refresh, ou {@code null} se não houver sessão salva ou o refresh falhar
     */
    public AuthResult tryRestore() {
        var savedRefreshToken = storage.secure().get(KEY_REFRESH_TOKEN);
        if (savedRefreshToken == null) {
            return null;
        }

        var result = refresh(savedRefreshToken);
        if (result == null) {
            // Token expirado ou inválido — limpar storage
            storage.secure().remove(KEY_ACCESS_TOKEN);
            storage.secure().remove(KEY_REFRESH_TOKEN);
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

        // Guarda access token para requisições subsequentes
        this.accessToken = result.accessToken();
        transport.setAccessTokenSupplier(() -> this.accessToken);

        // Persiste no storage seguro (AES-GCM) para sobreviver ao F5
        storage.secure().set(KEY_ACCESS_TOKEN, result.accessToken());
        storage.secure().set(KEY_REFRESH_TOKEN, result.refreshToken());

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
        transport.setAccessTokenSupplier(() -> this.accessToken);

        // Atualiza tokens no storage seguro (AES-GCM)
        storage.secure().set(KEY_ACCESS_TOKEN, result.accessToken());
        storage.secure().set(KEY_REFRESH_TOKEN, result.refreshToken());

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
            log.debug("logout: " + e.getMessage());
        }
        this.accessToken = null;
        transport.setAccessTokenSupplier(null);

        // Limpa tokens do storage seguro
        storage.secure().remove(KEY_ACCESS_TOKEN);
        storage.secure().remove(KEY_REFRESH_TOKEN);
    }

    @Override
    public SecurityContext resolveToken(String jwtToken) {
        throw new UnsupportedOperationException("resolveToken is server-side only; TeaVM clients use refresh tokens");
    }

    @Override
    public String createPersistentToken(Long userId, String userName) {
        throw new UnsupportedOperationException("createPersistentToken is server-side only; requires HMAC secret");
    }

    @Override
    public AuthResult loginWithPersistentToken(String persistentToken) {
        throw new UnsupportedOperationException("loginWithPersistentToken is server-side only; TeaVM clients use tryRestore()");
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
