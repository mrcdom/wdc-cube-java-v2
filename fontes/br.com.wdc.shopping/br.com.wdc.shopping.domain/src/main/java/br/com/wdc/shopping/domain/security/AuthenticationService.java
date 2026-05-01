package br.com.wdc.shopping.domain.security;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Serviço de autenticação compartilhado por todas as views.
 * <p>
 * Gerencia o fluxo challenge-response HMAC, sessões JWT,
 * e resolução de tokens para {@link SecurityContext}.
 */
public interface AuthenticationService {

	AtomicReference<AuthenticationService> BEAN = new AtomicReference<>();

	/**
	 * Gera um nonce de uso único para o fluxo challenge-response.
	 */
	ChallengeResult challenge();

	/**
	 * Autentica via HMAC challenge-response.
	 *
	 * @param userName nome do usuário
	 * @param digest   HMAC-SHA256(key=passwordHash, data=userName+nonce)
	 * @param nonce    nonce obtido via {@link #challenge()}
	 * @return resultado com tokens e chave pública, ou null se inválido
	 */
	AuthResult login(String userName, String digest, String nonce);

	/**
	 * Renova o access token usando o refresh token.
	 *
	 * @return resultado com novos tokens, ou null se refresh token inválido
	 */
	AuthResult refresh(String refreshToken);

	/**
	 * Encerra a sessão associada ao refresh token.
	 */
	void logout(String refreshToken);

	/**
	 * Valida um JWT e retorna o {@link SecurityContext} da sessão.
	 *
	 * @return contexto de segurança, ou null se token inválido/expirado
	 */
	SecurityContext resolveToken(String jwtToken);

}
