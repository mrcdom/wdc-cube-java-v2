package br.com.wdc.framework.domain.security;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Authentication service shared by all views.
 * <p>
 * Manages the HMAC challenge-response flow, JWT sessions, and token resolution into {@link SecurityContext}.
 */
public interface AuthenticationService {

    AtomicReference<AuthenticationService> BEAN = new AtomicReference<>();

    /**
     * Result of a successful login or refresh operation.
     */
    record AuthResult(
            Long userId,
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            String publicKey) {}

    /**
     * Result of a challenge request (nonce for HMAC login).
     */
    record ChallengeResult(String nonce, Instant expiresAt) {}

    /**
     * Generates a single-use nonce for the challenge-response flow.
     */
    ChallengeResult challenge();

    /**
     * Authenticates via HMAC challenge-response.
     *
     * @param userName user name
     * @param digest   HMAC-SHA256(key=passwordHash, data=userName+nonce)
     * @param nonce    nonce obtained via {@link #challenge()}
     * @return result with tokens and public key, or null if invalid
     */
    AuthResult login(String userName, String digest, String nonce);

    /**
     * Renews the access token using the refresh token.
     *
     * @return result with new tokens, or null if the refresh token is invalid
     */
    AuthResult refresh(String refreshToken);

    /**
     * Ends the session associated with the refresh token.
     */
    void logout(String refreshToken);

    /**
     * Validates a JWT and returns the session's {@link SecurityContext}.
     *
     * @return security context, or null if the token is invalid/expired
     */
    SecurityContext resolveToken(String jwtToken);

    /**
     * Creates a persistent (long-lived) token for "remember me". The token survives server restarts.
     *
     * @return JWT signed with a long TTL (30 days)
     */
    String createPersistentToken(Long userId, String userName);

    /**
     * Validates a persistent token and creates a full session. Used for auto-login on native clients
     * (desktop/mobile).
     *
     * @return result with tokens and an active session, or null if invalid/expired
     */
    AuthResult loginWithPersistentToken(String persistentToken);

}
