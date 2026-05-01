package br.com.wdc.shopping.domain.security;

import java.time.Instant;

/**
 * Resultado de uma operação de login ou refresh bem-sucedida.
 */
public record AuthResult(
		Long userId,
		String accessToken,
		String refreshToken,
		Instant expiresAt,
		String publicKey) {
}
