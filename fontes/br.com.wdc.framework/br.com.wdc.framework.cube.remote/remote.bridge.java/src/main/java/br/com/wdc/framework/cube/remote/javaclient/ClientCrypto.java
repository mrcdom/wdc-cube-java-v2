package br.com.wdc.framework.cube.remote.javaclient;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.SecretKeySpec;

import br.com.wdc.framework.cube.remote.javaclient.model.SecretContext;

/**
 * Handles the RSA + PBKDF2 + AES-GCM handshake required to connect to the Host.
 * <p>
 * Mirrors the Dart {@code DataSecurity} class from {@code remote.bridge.flutter}.
 * The {@code secret} field produced here must be sent in the first WebSocket message.
 */
public final class ClientCrypto {

    private static final SecureRandom RND = new SecureRandom();
    private static final int PBKDF2_ITERATIONS = 250000;
    private static final int AES_KEY_BITS = 256;

    private ClientCrypto() {}

    /**
     * Generates a fresh session secret from the server's RSA public key string.
     *
     * <p>The {@code appSKey} format is {@code "publicExponent_base36:publicKey_base36"},
     * as returned by {@code GET /api/session/init}.
     *
     * <p>The resulting {@link SecretContext} contains:
     * <ul>
     *   <li>{@code signature} — the {@code "secret"} field for the first WS message</li>
     *   <li>AES-256 key + IV — for deciphering tokens sent back by the server</li>
     * </ul>
     */
    public static SecretContext generateSecret(String appSKey) {
        try {
            // 1. Parse RSA public key  ("exponent:modulus" in base36)
            String[] parts = appSKey.split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid appSKey format, expected 'exponent:modulus' in base36");
            }
            BigInteger publicExponent = new BigInteger(parts[0], 36);
            BigInteger publicKey = new BigInteger(parts[1], 36);

            // 2. Generate random password: 12 random bytes → base64url (no padding) → UTF-8 bytes
            byte[] randomBytes = new byte[12];
            RND.nextBytes(randomBytes);
            String pwd = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
            byte[] pwdBytes = pwd.getBytes(StandardCharsets.UTF_8);

            // 3. Generate salt (16 bytes) and IV (12 bytes)
            byte[] salt = new byte[16];
            byte[] iv = new byte[12];
            RND.nextBytes(salt);
            RND.nextBytes(iv);

            // 4. Derive AES-256 key via PBKDF2-SHA256
            var spec = new PBEKeySpec(pwd.toCharArray(), salt, PBKDF2_ITERATIONS, AES_KEY_BITS);
            var keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            var aesKey = new SecretKeySpec(keyFactory.generateSecret(spec).getEncoded(), "AES");

            // 5. RSA-encrypt pwdBytes → base36
            String pwdBase64 = Base64.getEncoder().encodeToString(pwdBytes);
            byte[] pwdBase64Utf8 = pwdBase64.getBytes(StandardCharsets.UTF_8);
            BigInteger messageAsBigInt = new BigInteger(1, pwdBase64Utf8); // positive (unsigned)
            BigInteger encrypted = messageAsBigInt.modPow(publicExponent, publicKey);
            String encBase36 = encrypted.toString(36);

            // 6. Assemble signature: "<encryptedPwd>.<salt_b64url>.<iv_b64url>"
            var b64url = Base64.getUrlEncoder().withoutPadding();
            String signature = encBase36 + "." + b64url.encodeToString(salt) + "." + b64url.encodeToString(iv);

            return new SecretContext(signature, aesKey, iv);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate session secret", e);
        }
    }
}
