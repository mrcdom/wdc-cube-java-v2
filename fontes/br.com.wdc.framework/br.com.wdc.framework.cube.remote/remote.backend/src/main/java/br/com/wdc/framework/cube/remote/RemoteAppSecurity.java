package br.com.wdc.framework.cube.remote;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.commons.security.ChannelCrypto;

/**
 * What the remote host signs: session ids and the addresses it returns for the history.
 *
 * <p>
 * The signature is HMAC-SHA256 with a key only the server has, cut to 16 bytes and written in Base62. A client cannot
 * make a session id or an address of its own that the host accepts. The key comes from the configuration, so that a
 * restart, or another instance with the same key, accepts what was signed before.
 * </p>
 *
 * <p>
 * Each application module has its own instance — no shared global state.
 * </p>
 */
public final class RemoteAppSecurity {

    /** The system property that carries the key, Base64 URL-safe, at least 32 bytes. */
    public static final String SIGNING_KEY_PROPERTY = "wdc.remote.signing_key";

    private static final int SIGNATURE_LENGTH = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final byte[] signingKey;

    private RemoteAppSecurity(byte[] signingKey) {
        if (signingKey == null || signingKey.length < 32) {
            throw new IllegalArgumentException("The remote signing key must have at least 32 bytes");
        }
        this.signingKey = signingKey.clone();
    }

    /** With the given key (at least 32 bytes). */
    public static RemoteAppSecurity withKey(byte[] signingKey) {
        return new RemoteAppSecurity(signingKey);
    }

    /**
     * With the key in the system property {@value #SIGNING_KEY_PROPERTY}.
     *
     * @throws IllegalStateException if it is not set
     */
    public static RemoteAppSecurity fromSystemProperty() {
        var encoded = System.getProperty(SIGNING_KEY_PROPERTY);
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException("The remote signing key is not configured (" + SIGNING_KEY_PROPERTY + ")");
        }
        return new RemoteAppSecurity(Base64.getUrlDecoder().decode(encoded.trim()));
    }

    /** A new random key, Base64 URL-safe, for the configuration. */
    public static String newEncodedKey() {
        var key = new byte[32];
        RANDOM.nextBytes(key);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
    }

    // :: Session ids

    /** A new session id: 32 random bytes and their signature, {@code base62(random) + "." + signature}. */
    public String newSessionId() {
        var random = new byte[32];
        RANDOM.nextBytes(random);
        var part1 = Base62.BEAN.encodeToString(random);
        return part1 + "." + sign(part1);
    }

    /** Whether the text is a session id this host (or one with the same key) made. */
    public boolean isSessionId(String sessionId) {
        if (sessionId == null) {
            return false;
        }
        var dot = sessionId.indexOf('.');
        if (dot <= 0 || dot != sessionId.lastIndexOf('.')) {
            return false;
        }
        return verify(sessionId.substring(0, dot), sessionId.substring(dot + 1));
    }

    // :: Signatures

    /** The signature of a text: HMAC-SHA256, its first 16 bytes, in Base62. */
    public String sign(String content) {
        var mac = ChannelCrypto.hmacSha256(signingKey, content.getBytes(StandardCharsets.UTF_8));
        return Base62.BEAN.encodeToString(Arrays.copyOf(mac, SIGNATURE_LENGTH));
    }

    /** Whether {@code signature} is the signature of {@code content}, compared in constant time. */
    public boolean verify(String content, String signature) {
        if (content == null || signature == null) {
            return false;
        }
        var expected = sign(content).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
    }
}
