package br.com.wdc.framework.domain.security;

import java.util.Base64;

/**
 * The stored form of a password: a SCRAM-SHA-256 verifier ({@link Scram}), in PostgreSQL's encoding —
 * {@code SCRAM-SHA-256$<iterations>:<salt>$<StoredKey>:<ServerKey>}, each binary part in Base64.
 */
public final class ScramVerifier {

    public static final String MECHANISM = "SCRAM-SHA-256";

    /**
     * The iteration count of new verifiers: RFC 7677's minimum and PostgreSQL's default. Every client derives the key
     * in its own code, on the user's device; a verifier keeps its own count, so it can be raised later.
     */
    public static final int DEFAULT_ITERATIONS = 4096;

    /** Bytes of a new salt. */
    public static final int SALT_SIZE = 16;

    private final int iterations;
    private final byte[] salt;
    private final byte[] storedKey;
    private final byte[] serverKey;

    public ScramVerifier(int iterations, byte[] salt, byte[] storedKey, byte[] serverKey) {
        this.iterations = iterations;
        this.salt = salt.clone();
        this.storedKey = storedKey.clone();
        this.serverKey = serverKey.clone();
    }

    public int iterations() {
        return iterations;
    }

    public byte[] salt() {
        return salt.clone();
    }

    public byte[] storedKey() {
        return storedKey.clone();
    }

    public byte[] serverKey() {
        return serverKey.clone();
    }

    public String encode() {
        var b64 = Base64.getEncoder();
        return MECHANISM + "$" + iterations + ":" + b64.encodeToString(salt) + "$" + b64.encodeToString(storedKey) + ":"
                + b64.encodeToString(serverKey);
    }

    /** Reads a stored verifier; {@code null} when {@code text} is not one. */
    public static ScramVerifier parse(String text) {
        if (text == null) {
            return null;
        }
        var parts = text.trim().split("\\$", -1);
        if (parts.length != 3 || !MECHANISM.equals(parts[0])) {
            return null;
        }
        var params = parts[1].split(":", -1);
        var keys = parts[2].split(":", -1);
        if (params.length != 2 || keys.length != 2) {
            return null;
        }
        try {
            var b64 = Base64.getDecoder();
            int iterations = Integer.parseInt(params[0]);
            if (iterations <= 0) {
                return null;
            }
            return new ScramVerifier(iterations, b64.decode(params[1]), b64.decode(keys[0]), b64.decode(keys[1]));
        } catch (IllegalArgumentException notAVerifier) {
            return null;
        }
    }
}
