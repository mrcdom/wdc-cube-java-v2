package br.com.wdc.shopping.domain.security;

import java.time.Instant;

/**
 * Resultado de um pedido de challenge (nonce para login HMAC).
 */
public record ChallengeResult(String nonce, Instant expiresAt) {
}
