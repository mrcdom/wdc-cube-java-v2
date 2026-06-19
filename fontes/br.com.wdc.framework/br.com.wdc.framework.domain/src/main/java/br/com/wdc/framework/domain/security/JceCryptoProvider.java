package br.com.wdc.framework.domain.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Implementação de {@link CryptoProvider} usando JCE (Java Cryptography Extension).
 * Usada pelo servidor e clients desktop/mobile.
 */
public class JceCryptoProvider implements CryptoProvider {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Override
    public byte[] md5(byte[] input) {
        try {
            return MessageDigest.getInstance("MD5").digest(input);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError("MD5 not available", e);
        }
    }

    @Override
    public byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            var mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, HMAC_ALGORITHM));
            return mac.doFinal(data);
        } catch (Exception e) {
            throw new AssertionError("HMAC-SHA256 failed", e);
        }
    }

}
