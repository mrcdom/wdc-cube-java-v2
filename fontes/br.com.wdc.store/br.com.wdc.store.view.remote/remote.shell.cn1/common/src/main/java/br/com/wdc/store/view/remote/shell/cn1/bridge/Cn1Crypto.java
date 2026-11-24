package br.com.wdc.store.view.remote.shell.cn1.bridge;

import com.codename1.security.Cipher;
import com.codename1.security.Hmac;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.util.Base64;
import com.codename1.util.BigInteger;

/**
 * Bridge handshake on Codename One — RSA (textbook, base36) + PBKDF2-HMAC-SHA256 + AES-256-GCM.
 *
 * <p>
 * Reproduces, with native CN1 APIs, exactly the Java bridge's {@code ClientCrypto}/{@code SecretContext}
 * (and Flutter's {@code DataSecurity}). Recipe validated end to end by a plain Java probe with a real
 * login (admin/admin) against the server.
 * </p>
 *
 * <ul>
 * <li>{@link #signature()} — value of the {@code "secret"} field of the 1st WS message;</li>
 * <li>{@link #encipher(String)} — enciphers a value (e.g.: {@code p.password}) with the session AES key.</li>
 * </ul>
 */
public final class Cn1Crypto {

    private static final int PBKDF2_ITERATIONS = 250000;
    private static final int AES_KEY_BYTES = 32; // 256 bits

    private final String signature;
    private final byte[] aesKey;
    private final byte[] iv;

    private Cn1Crypto(String signature, byte[] aesKey, byte[] iv) {
        this.signature = signature;
        this.aesKey = aesKey;
        this.iv = iv;
    }

    public String signature() {
        return signature;
    }

    /**
     * Generates the session secret from the server's RSA public key
     * ({@code appSKey} = {@code "exponent:modulus"} in base36, from {@code /api/session/init}).
     */
    public static Cn1Crypto generate(String appSKey) {
        int colon = appSKey.indexOf(':');
        BigInteger publicExponent = new BigInteger(appSKey.substring(0, colon), 36);
        BigInteger publicModulus = new BigInteger(appSKey.substring(colon + 1), 36);

        // random password: 12 bytes -> base64url without padding
        byte[] randomBytes = SecureRandom.bytes(12);
        String pwd = stripPadding(Base64.encodeUrlSafe(randomBytes));
        byte[] pwdBytes = utf8(pwd);

        byte[] salt = SecureRandom.bytes(16);
        byte[] iv = SecureRandom.bytes(12);

        // AES-256 via PBKDF2-HMAC-SHA256
        byte[] aesKey = pbkdf2(pwdBytes, salt, PBKDF2_ITERATIONS, AES_KEY_BYTES);

        // textbook RSA: encrypts the bytes of base64(pwdBytes); base36
        String pwdBase64 = Base64.encodeNoNewline(pwdBytes); // standard base64 (with padding)
        BigInteger message = new BigInteger(1, utf8(pwdBase64)); // positive (unsigned)
        BigInteger encrypted = message.modPow(publicExponent, publicModulus);

        String signature = encrypted.toString(36)
                + "." + stripPadding(Base64.encodeUrlSafe(salt))
                + "." + stripPadding(Base64.encodeUrlSafe(iv));

        return new Cn1Crypto(signature, aesKey, iv);
    }

    /** Enciphers a text with the session's AES-256-GCM (128-bit tag) and returns standard base64. */
    public String encipher(String plaintext) {
        byte[] out = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", aesKey), iv, new byte[0], utf8(plaintext));
        return Base64.encodeNoNewline(out);
    }

    /** Deciphers a base64 enciphered by the session (inverse of {@link #encipher}); {@code null} if invalid. */
    public String decipher(String b64) {
        try {
            byte[] in = Base64.decode(utf8(b64));
            byte[] out = Cipher.aesDecrypt(Cipher.AES_GCM, new SecretKey("AES", aesKey), iv, new byte[0], in);
            return new String(out, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    // -- PBKDF2-HMAC-SHA256 (dkLen == 32 == 1 HMAC-SHA256 block) --


    private static byte[] pbkdf2(byte[] password, byte[] salt, int iterations, int dkLen) {
        byte[] block = new byte[salt.length + 4];
        System.arraycopy(salt, 0, block, 0, salt.length);
        block[salt.length + 3] = 1; // INT_32_BE(1)

        byte[] u = Hmac.sha256(password, block);
        byte[] t = new byte[u.length];
        System.arraycopy(u, 0, t, 0, u.length);

        for (int i = 1; i < iterations; i++) {
            u = Hmac.sha256(password, u);
            for (int j = 0; j < t.length; j++) {
                t[j] = (byte) (t[j] ^ u[j]);
            }
        }

        if (dkLen == t.length) {
            return t;
        }
        byte[] dk = new byte[dkLen];
        System.arraycopy(t, 0, dk, 0, dkLen);
        return dk;
    }

    private static String stripPadding(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '=') {
            end--;
        }
        return s.substring(0, end);
    }

    private static byte[] utf8(String s) {
        try {
            return s.getBytes("UTF-8");
        } catch (Exception e) {
            return s.getBytes();
        }
    }
}
