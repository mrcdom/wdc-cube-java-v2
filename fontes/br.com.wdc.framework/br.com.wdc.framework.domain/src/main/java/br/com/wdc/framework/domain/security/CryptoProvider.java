package br.com.wdc.framework.domain.security;

import java.util.concurrent.atomic.AtomicReference;

/**
 * SPI for the cryptographic operations needed by authentication.
 * <p>
 * Allows alternative implementations (e.g. the Web Crypto API in the browser)
 * to replace the default JCE implementation.
 * <p>
 * If no provider is registered, {@link PasswordUtil} uses the JVM's JCE
 * implementation (MessageDigest + javax.crypto.Mac).
 */
public interface CryptoProvider {

    AtomicReference<CryptoProvider> BEAN = new AtomicReference<>();

    /**
     * MD5 hash of bytes, returning the digest bytes.
     */
    byte[] md5(byte[] input);

    /**
     * SHA-256 hash of bytes, returning the digest bytes.
     */
    byte[] sha256(byte[] input);

    /**
     * HMAC-SHA256.
     *
     * @param key  key bytes
     * @param data data bytes
     * @return HMAC bytes
     */
    byte[] hmacSha256(byte[] key, byte[] data);

}
