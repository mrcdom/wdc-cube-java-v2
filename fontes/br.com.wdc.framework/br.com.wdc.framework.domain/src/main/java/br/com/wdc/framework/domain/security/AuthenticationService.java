package br.com.wdc.framework.domain.security;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Signs users in and keeps their sessions, for all views. On the server it is the implementation itself; in a client,
 * a proxy to the server's {@code /api/auth}.
 * <p>
 * The sign-in is a SCRAM-SHA-256 exchange ({@link Scram}): {@link #challenge(String)} gives a single-use nonce and the
 * parameters of the user's verifier, the client answers with its proof ({@link #login}), and the server proves back,
 * with its signature, that it holds the verifier.
 */
public interface AuthenticationService {

    AtomicReference<AuthenticationService> BEAN = new AtomicReference<>();

    /**
     * Result of a successful login or refresh operation.
     *
     * @param serverSignature the server's SCRAM signature (Base64) over the exchange: what the client checks against
     *                        {@link Scram.ClientResponse#serverIsAuthentic}; {@code null} on a refresh or a sign-in by
     *                        persistent token, which prove nothing about the password
     */
    record AuthResult(
            Long userId,
            String accessToken,
            String refreshToken,
            Instant expiresAt,
            String publicKey,
            String serverSignature) {}

    /**
     * The server's challenge for one user's sign-in: a single-use {@code nonce} and the parameters of the user's
     * verifier — the {@code salt} (Base64) and the {@code iterations} — so the client derives the same key. An unknown
     * user gets made-up parameters, the same every time, so the answer does not tell whether the user exists.
     */
    record ChallengeResult(String nonce, String salt, int iterations, Instant expiresAt) {}

    /** Starts {@code userName}'s sign-in: a single-use nonce and the parameters of the user's verifier. */
    ChallengeResult challenge(String userName);

    /**
     * Ends the sign-in.
     *
     * @param userName user name the challenge was made for
     * @param proof    the client's SCRAM proof (Base64), {@link Scram.ClientResponse#proof()}
     * @param nonce    nonce of the {@link #challenge(String)} made for {@code userName}
     * @return result with tokens, public key and server signature; {@code null} when the proof is wrong, the nonce is
     *         unknown, expired or someone else's, or the user does not exist — the same answer for all
     * @throws br.com.wdc.framework.domain.exception.TooManyRequestsException when the user failed too often and must
     *                                                                       wait, with the seconds to wait
     */
    AuthResult login(String userName, String proof, String nonce);

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
