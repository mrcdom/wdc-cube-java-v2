package br.com.wdc.shopping.domain.security;

import java.time.Instant;

/**
 * Resultado de um pedido de challenge (nonce para login HMAC).
 * <p>
 * Nota: não é um {@code record} para compatibilidade com o compilador AOT
 * do RoboVM (Soot não suporta o atributo Record do bytecode).
 */
public final class ChallengeResult {

    private final String nonce;
    private final Instant expiresAt;

    public ChallengeResult(String nonce, Instant expiresAt) {
        this.nonce = nonce;
        this.expiresAt = expiresAt;
    }

    public String nonce() { return nonce; }
    public Instant expiresAt() { return expiresAt; }
}
