package br.com.wdc.framework.domain.security;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * The login's challenge and response: SCRAM-SHA-256 (RFC 5802, RFC 7677), the same scheme PostgreSQL uses.
 * <p>
 * The server keeps only a {@link ScramVerifier} per user: a salt, an iteration count and two keys derived from the
 * password. Neither key lets anyone sign in, so a leak of the user table gives no access; guessing the password from
 * it costs one PBKDF2 run per guess. At sign-in the server sends the salt, the iteration count and a single-use nonce;
 * the client proves it knows the password without sending it ({@link #clientProof}), and the server answers with its
 * own signature ({@link #serverSignature}), proving it holds the verifier.
 * <p>
 * The password goes in as its MD5 digest ({@link PasswordUtil#hashPassword}): that is what lets a database migration
 * turn stored digests into verifiers without knowing the passwords. The digest adds nothing to the security, and takes
 * nothing from it: what protects the password is the salted PBKDF2.
 * <p>
 * Every computation goes through the {@link CryptoProvider}, so the same code runs on the server and in every client
 * (the browser's included).
 */
public final class Scram {

    private static final byte[] CLIENT_KEY = "Client Key".getBytes(StandardCharsets.UTF_8);
    private static final byte[] SERVER_KEY = "Server Key".getBytes(StandardCharsets.UTF_8);

    private final CryptoProvider crypto;

    public Scram(CryptoProvider crypto) {
        this.crypto = crypto;
    }

    /** On the provider set for the process ({@link CryptoProvider#BEAN}). */
    public static Scram current() {
        var crypto = CryptoProvider.BEAN.get();
        if (crypto == null) {
            throw new IllegalStateException("CryptoProvider.BEAN not initialized");
        }
        return new Scram(crypto);
    }

    /** What both sides sign: the user and the server's nonce, so a proof is good for one user and one exchange. */
    public static String authMessage(String userName, String nonce) {
        return "n=" + userName + ",r=" + nonce;
    }

    /** {@code PBKDF2-HMAC-SHA256(password digest, salt, iterations)}, 32 bytes: the root of every key. */
    public byte[] saltedPassword(String passwordDigest, byte[] salt, int iterations) {
        if (iterations <= 0) {
            throw new IllegalArgumentException("The iteration count must be positive");
        }
        var password = passwordDigest.getBytes(StandardCharsets.UTF_8);
        // a single block of PBKDF2: the key is as long as the hash
        var first = new byte[salt.length + 4];
        System.arraycopy(salt, 0, first, 0, salt.length);
        first[salt.length + 3] = 1;
        var u = crypto.hmacSha256(password, first);
        var result = u.clone();
        for (int n = 1; n < iterations; n++) {
            u = crypto.hmacSha256(password, u);
            for (int i = 0; i < result.length; i++) {
                result[i] ^= u[i];
            }
        }
        return result;
    }

    public byte[] clientKey(byte[] saltedPassword) {
        return crypto.hmacSha256(saltedPassword, CLIENT_KEY);
    }

    public byte[] serverKey(byte[] saltedPassword) {
        return crypto.hmacSha256(saltedPassword, SERVER_KEY);
    }

    public byte[] storedKey(byte[] clientKey) {
        return crypto.sha256(clientKey);
    }

    /** The verifier the server keeps for a password digest. */
    public ScramVerifier verifier(String passwordDigest, byte[] salt, int iterations) {
        var salted = saltedPassword(passwordDigest, salt, iterations);
        return new ScramVerifier(iterations, salt, storedKey(clientKey(salted)), serverKey(salted));
    }

    /** The client's proof: {@code ClientKey XOR HMAC(StoredKey, AuthMessage)}. It reveals neither the key nor the password. */
    public byte[] clientProof(byte[] saltedPassword, String authMessage) {
        var clientKey = clientKey(saltedPassword);
        var signature = crypto.hmacSha256(storedKey(clientKey), authMessage.getBytes(StandardCharsets.UTF_8));
        return xor(clientKey, signature);
    }

    /** The server's signature over the exchange: only who holds the verifier can compute it. */
    public byte[] serverSignature(byte[] serverKey, String authMessage) {
        return crypto.hmacSha256(serverKey, authMessage.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Whether {@code proof} shows the client knows the password of {@code verifier}: the key it carries, hashed, must
     * be the stored one. Compared in constant time.
     */
    public boolean verifyProof(ScramVerifier verifier, String authMessage, byte[] proof) {
        if (proof == null || proof.length != verifier.storedKey().length) {
            return false;
        }
        var signature = crypto.hmacSha256(verifier.storedKey(), authMessage.getBytes(StandardCharsets.UTF_8));
        var clientKey = xor(proof, signature);
        return constantTimeEquals(storedKey(clientKey), verifier.storedKey());
    }

    /**
     * Whether {@code password} is the one of {@code verifier}, without an exchange: for a process that holds the
     * verifier itself.
     */
    public boolean matches(ScramVerifier verifier, String password) {
        var salted = saltedPassword(PasswordUtil.hashPassword(password, crypto), verifier.salt(), verifier.iterations());
        return constantTimeEquals(storedKey(clientKey(salted)), verifier.storedKey());
    }

    /**
     * The client's whole side of the exchange, for the challenge the server sent: the proof to send and the server
     * signature to expect back.
     */
    public ClientResponse respond(String userName, String password, AuthenticationService.ChallengeResult challenge) {
        var salted = saltedPassword(PasswordUtil.hashPassword(password, crypto),
                Base64.getDecoder().decode(challenge.salt()), challenge.iterations());
        var authMessage = authMessage(userName, challenge.nonce());
        return new ClientResponse(
                Base64.getEncoder().encodeToString(clientProof(salted, authMessage)),
                Base64.getEncoder().encodeToString(serverSignature(serverKey(salted), authMessage)));
    }

    /** What the client sends ({@code proof}) and what it must get back from the server ({@code expectedServerSignature}). */
    public record ClientResponse(String proof, String expectedServerSignature) {

        /** Whether the server's signature is the expected one, compared in constant time. */
        public boolean serverIsAuthentic(String serverSignature) {
            if (serverSignature == null) {
                return false;
            }
            return constantTimeEquals(serverSignature.getBytes(StandardCharsets.UTF_8),
                    expectedServerSignature.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** Equality that takes the same time wherever the first difference is. */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }

    private static byte[] xor(byte[] a, byte[] b) {
        var out = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = (byte) (a[i] ^ b[i]);
        }
        return out;
    }
}
