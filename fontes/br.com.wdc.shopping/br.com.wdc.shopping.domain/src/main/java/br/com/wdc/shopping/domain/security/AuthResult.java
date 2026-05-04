package br.com.wdc.shopping.domain.security;

import java.time.Instant;

/**
 * Resultado de uma operação de login ou refresh bem-sucedida.
 * <p>
 * Nota: não é um {@code record} para compatibilidade com o compilador AOT
 * do RoboVM (Soot não suporta o atributo Record do bytecode).
 */
public final class AuthResult {

    private final Long userId;
    private final String accessToken;
    private final String refreshToken;
    private final Instant expiresAt;
    private final String publicKey;

    public AuthResult(Long userId, String accessToken, String refreshToken, Instant expiresAt, String publicKey) {
        this.userId = userId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.publicKey = publicKey;
    }

    public Long userId() { return userId; }
    public String accessToken() { return accessToken; }
    public String refreshToken() { return refreshToken; }
    public Instant expiresAt() { return expiresAt; }
    public String publicKey() { return publicKey; }
}
