package br.com.wdc.store.view.remote.shell.cn1.bridge;

import com.codename1.security.Cipher;
import com.codename1.security.Hash;
import com.codename1.security.Hmac;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.util.Base64;
import com.codename1.util.BigInteger;

/**
 * The cryptography of the remote channel, protocol version 1, on Codename One (specification:
 * {@code docs/bridge-skeleton-protocol.md}, section 6).
 *
 * <ul>
 * <li><b>Key agreement</b>, per connection: {@link #newHandshake()} makes an ephemeral P-256 key pair
 * ({@link P256}); its {@link Handshake#clientKey()} is the Base64 SubjectPublicKeyInfo the client sends, and
 * {@link Handshake#agree(String, String)} takes the host's {@code serverKey} and derives the key.</li>
 * <li><b>Connection key</b>: HKDF-SHA256 (RFC 5869) over the ECDH x-coordinate, with the {@code appId} (UTF-8) as
 * salt and {@value #KEY_INFO} as info, 32 bytes, used as an AES-256 key.</li>
 * <li><b>A ciphered value</b>: AES-256-GCM with a 128-bit tag and a random 12-byte IV of its own, written as
 * {@code Base64(IV ‖ ciphertext ‖ tag)} ({@link #encipher}, {@link #decipher}).</li>
 * </ul>
 */
public final class Cn1Crypto {

    /** The HKDF info that names what the derived key is for. */
    public static final String KEY_INFO = "wdc-cube-remote/aes-256-gcm/v1";

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH = 16;
    private static final int KEY_LENGTH = 32;
    private static final int SHA256_LENGTH = 32;

    private final SecretKey key;

    Cn1Crypto(byte[] key) {
        if (key == null || key.length != KEY_LENGTH) {
            throw new IllegalArgumentException("An AES-256 key has 32 bytes");
        }
        this.key = new SecretKey("AES", key);
    }

    /** The client's half of a connection's handshake: a new ephemeral P-256 key pair. */
    public static Handshake newHandshake() {
        return new Handshake(P256.newPrivateKey());
    }

    /** One connection's key agreement: sends {@link #clientKey()}, then {@link #agree} with the answer. */
    public static final class Handshake {

        private final BigInteger privateKey;
        private final String clientKey;

        Handshake(BigInteger privateKey) {
            this.privateKey = privateKey;
            this.clientKey = Base64.encodeNoNewline(P256.publicKey(privateKey));
        }

        /** This end's public key, Base64 of its X.509 SubjectPublicKeyInfo. */
        public String clientKey() {
            return clientKey;
        }

        /**
         * The connection key, from the host's public key and the session id.
         *
         * @throws IllegalArgumentException if {@code serverKey} is not a P-256 public key in Base64
         *                                  SubjectPublicKeyInfo
         */
        public Cn1Crypto agree(String serverKey, String appId) {
            byte[] spki = serverKey != null ? Base64.decode(utf8(serverKey)) : null;
            if (spki == null) {
                throw new IllegalArgumentException("The server key is not Base64");
            }
            byte[] shared = P256.sharedSecret(privateKey, spki);
            try {
                return new Cn1Crypto(hkdfSha256(shared, utf8(appId), utf8(KEY_INFO), KEY_LENGTH));
            } finally {
                fill(shared);
            }
        }
    }

    /** Ciphers a text with the connection key: {@code Base64(IV ‖ ciphertext ‖ tag)}, with a random IV. */
    public String encipher(String plaintext) {
        byte[] iv = SecureRandom.bytes(IV_LENGTH);
        byte[] sealed = Cipher.aesEncrypt(Cipher.AES_GCM, key, iv, null, utf8(plaintext));
        byte[] out = new byte[IV_LENGTH + sealed.length];
        System.arraycopy(iv, 0, out, 0, IV_LENGTH);
        System.arraycopy(sealed, 0, out, IV_LENGTH, sealed.length);
        return Base64.encodeNoNewline(out);
    }

    /**
     * Deciphers a value made with this connection's key (by either end); {@code null} when it is not Base64, is
     * too short, or does not authenticate (tampered, or ciphered with another key).
     */
    public String decipher(String value) {
        byte[] in = value != null ? Base64.decode(utf8(value)) : null;
        if (in == null || in.length < IV_LENGTH + TAG_LENGTH) {
            return null;
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] sealed = new byte[in.length - IV_LENGTH];
        System.arraycopy(in, 0, iv, 0, IV_LENGTH);
        System.arraycopy(in, IV_LENGTH, sealed, 0, sealed.length);
        try {
            return new String(Cipher.aesDecrypt(Cipher.AES_GCM, key, iv, null, sealed), "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    /** HKDF with HMAC-SHA256 (RFC 5869): extract, then expand to {@code length} bytes. */
    static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int length) {
        if (length < 0 || length > 255 * SHA256_LENGTH) {
            throw new IllegalArgumentException("HKDF-SHA256 length out of range");
        }
        byte[] prk = Hmac.sha256(salt == null || salt.length == 0 ? new byte[SHA256_LENGTH] : salt, ikm);
        byte[] out = new byte[length];
        byte[] block = new byte[0];
        int written = 0;
        for (int counter = 1; written < length; counter++) {
            Hmac mac = Hmac.create(Hash.SHA256, prk);
            mac.update(block);
            mac.update(info);
            mac.update(new byte[] { (byte) counter });
            block = mac.doFinal();
            int n = Math.min(block.length, length - written);
            System.arraycopy(block, 0, out, written, n);
            written += n;
        }
        fill(prk);
        return out;
    }

    private static void fill(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = 0;
        }
    }

    static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 is not available", e);
        }
    }
}
