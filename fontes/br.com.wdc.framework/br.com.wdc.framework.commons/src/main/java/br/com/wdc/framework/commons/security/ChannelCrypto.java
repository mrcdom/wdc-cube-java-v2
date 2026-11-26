package br.com.wdc.framework.commons.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * The cryptography of the remote channel, version 1, as the JVM ends (the host and the Java bridge) do it.
 *
 * <ul>
 * <li><b>Key agreement</b>, per connection: each end makes an ephemeral ECDH P-256 key pair
 * ({@link #newKeyPair()}) and sends its public key as Base64 of the X.509 SubjectPublicKeyInfo
 * ({@link #encodePublicKey}).</li>
 * <li><b>Session key</b>: HKDF-SHA256 over the ECDH shared secret, with the session id (UTF-8) as salt and
 * {@value #KEY_INFO} as info, 32 bytes, used as an AES-256 key ({@link #deriveKey}).</li>
 * <li><b>A ciphered value</b>: AES-256-GCM with a 128-bit tag and a random 12-byte IV of its own, written as
 * {@code Base64(IV ‖ ciphertext ‖ tag)} ({@link #cipher}, {@link #decipher}).</li>
 * </ul>
 *
 * <p>
 * Every other end (the browser bridges, Flutter, Codename One) does the same with its platform's primitives; the
 * specification is {@code docs/bridge-skeleton-protocol.md}, section 6.
 * </p>
 */
public final class ChannelCrypto {

    /** The HKDF info that names what the derived key is for. */
    public static final String KEY_INFO = "wdc-cube-remote/aes-256-gcm/v1";

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_LENGTH = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private ChannelCrypto() {
        // NOOP
    }

    /** A new ephemeral ECDH P-256 key pair. */
    public static KeyPair newKeyPair() {
        try {
            var generator = KeyPairGenerator.getInstance("EC");
            generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException caught) {
            throw new IllegalStateException("P-256 is not available", caught);
        }
    }

    /** The public key as Base64 of its X.509 SubjectPublicKeyInfo. */
    public static String encodePublicKey(PublicKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    /**
     * Reads a peer's public key.
     *
     * @throws IllegalArgumentException if it is not a P-256 public key in Base64 SubjectPublicKeyInfo
     */
    public static PublicKey decodePublicKey(String encoded) {
        try {
            var bytes = Base64.getDecoder().decode(encoded);
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(bytes));
        } catch (IllegalArgumentException | GeneralSecurityException caught) {
            throw new IllegalArgumentException("Not a P-256 public key", caught);
        }
    }

    /** The session key both ends derive: HKDF-SHA256 over the ECDH secret, salted with the session id. */
    public static byte[] deriveKey(PrivateKey own, PublicKey peer, String sessionId) {
        try {
            var agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(own);
            agreement.doPhase(peer, true);
            var shared = agreement.generateSecret();
            try {
                return hkdfSha256(shared, sessionId.getBytes(StandardCharsets.UTF_8),
                        KEY_INFO.getBytes(StandardCharsets.UTF_8), KEY_LENGTH);
            } finally {
                Arrays.fill(shared, (byte) 0);
            }
        } catch (GeneralSecurityException caught) {
            throw new IllegalArgumentException("Key agreement failed", caught);
        }
    }

    /** HKDF with HMAC-SHA256 (RFC 5869): extract, then expand to {@code length} bytes. */
    public static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(salt.length == 0 ? new byte[32] : salt, "HmacSHA256"));
            var prk = mac.doFinal(ikm);

            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            var out = ByteBuffer.allocate(length);
            var block = new byte[0];
            for (int counter = 1; out.hasRemaining(); counter++) {
                mac.update(block);
                mac.update(info);
                mac.update((byte) counter);
                block = mac.doFinal();
                out.put(block, 0, Math.min(block.length, out.remaining()));
            }
            return out.array();
        } catch (GeneralSecurityException caught) {
            throw new IllegalStateException("HmacSHA256 is not available", caught);
        }
    }

    /** Ciphers a text with the session key: {@code Base64(IV ‖ ciphertext ‖ tag)}, with a random IV. */
    public static String cipher(byte[] key, String text) {
        try {
            var iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            var sealed = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            var out = new byte[IV_LENGTH + sealed.length];
            System.arraycopy(iv, 0, out, 0, IV_LENGTH);
            System.arraycopy(sealed, 0, out, IV_LENGTH, sealed.length);
            return Base64.getEncoder().encodeToString(out);
        } catch (GeneralSecurityException caught) {
            throw new IllegalStateException("AES-GCM failed", caught);
        }
    }

    /**
     * Deciphers a value made by {@link #cipher}, at any end.
     *
     * @throws IllegalArgumentException if it is not Base64, is too short, or fails authentication (tampered, or
     *                                  ciphered with another key)
     */
    public static String decipher(byte[] key, String value) {
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException caught) {
            throw new IllegalArgumentException("A ciphered value is Base64", caught);
        }
        if (bytes.length < IV_LENGTH + TAG_BITS / 8) {
            throw new IllegalArgumentException("A ciphered value is too short");
        }
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"),
                    new GCMParameterSpec(TAG_BITS, bytes, 0, IV_LENGTH));
            var plain = cipher.doFinal(bytes, IV_LENGTH, bytes.length - IV_LENGTH);
            return new String(plain, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException caught) {
            throw new IllegalArgumentException("A ciphered value did not authenticate", caught);
        }
    }

    /** HMAC-SHA256 of {@code data} with {@code key}. */
    public static byte[] hmacSha256(byte[] key, byte[] data) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException caught) {
            throw new IllegalStateException("HmacSHA256 is not available", caught);
        }
    }
}
