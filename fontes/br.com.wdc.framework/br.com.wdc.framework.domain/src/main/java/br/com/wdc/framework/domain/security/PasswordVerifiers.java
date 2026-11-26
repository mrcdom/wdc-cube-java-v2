package br.com.wdc.framework.domain.security;

import java.security.SecureRandom;

/**
 * What the server stores for a password: a SCRAM-SHA-256 verifier with a fresh random salt ({@link ScramVerifier}).
 * The only place that makes one — the user API, the seed data and the conversion of stored digests use it.
 */
public final class PasswordVerifiers {

    private static final CryptoProvider CRYPTO = new JceCryptoProvider();
    private static final Scram SCRAM = new Scram(CRYPTO);
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordVerifiers() {
    }

    /** The verifier of a password as the user typed it. */
    public static String fromPassword(String plain) {
        return fromDigest(PasswordUtil.hashPassword(plain, CRYPTO), ScramVerifier.DEFAULT_ITERATIONS);
    }

    /** The verifier of a password known only by its MD5 digest ({@link PasswordUtil#hashPassword}). */
    public static String fromDigest(String digest) {
        return fromDigest(digest, ScramVerifier.DEFAULT_ITERATIONS);
    }

    public static String fromDigest(String digest, int iterations) {
        var salt = new byte[ScramVerifier.SALT_SIZE];
        RANDOM.nextBytes(salt);
        return SCRAM.verifier(digest.trim(), salt, iterations).encode();
    }
}
