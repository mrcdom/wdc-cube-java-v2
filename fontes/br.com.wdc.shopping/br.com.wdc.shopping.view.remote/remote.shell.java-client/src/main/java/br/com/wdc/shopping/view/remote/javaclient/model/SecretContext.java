package br.com.wdc.shopping.view.remote.javaclient.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Holds the AES-GCM key material derived during the handshake.
 * <p>
 * Produced by {@link ClientCrypto#generateSecret(String)}.
 * The {@code signature} field is the {@code "secret"} value for the first WS message.
 * The AES key + IV are used to decipher tokens sent back by the server.
 */
public final class SecretContext {

    private final String signature;
    private final SecretKeySpec aesKey;
    private final byte[] iv;

    public SecretContext(String signature, SecretKeySpec aesKey, byte[] iv) {
        this.signature = signature;
        this.aesKey = aesKey;
        this.iv = iv;
    }

    /** The {@code "secret"} value to include in the first WebSocket message. */
    public String signature() {
        return signature;
    }

    /**
     * Deciphers an AES-256-GCM encrypted, base64-encoded value sent by the server.
     * Used for the {@code accessToken} field in server responses.
     */
    public String decipher(String b64CipheredText) {
        try {
            byte[] cipherBytes = Base64.getDecoder().decode(b64CipheredText);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decipher server token", e);
        }
    }

    /**
     * Enciphers a plain-text value with the session AES-256-GCM key, returning a base64 string.
     * <p>
     * Used to encrypt sensitive parameters (e.g., {@code p.password}) before sending them
     * to the server, which decrypts them with {@code app.b64Decipher()}.
     */
    public String encipher(String plaintext) {
        try {
            byte[] data = plaintext.getBytes(StandardCharsets.UTF_8);
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(128, iv));
            return Base64.getEncoder().encodeToString(cipher.doFinal(data));
        } catch (Exception e) {
            throw new RuntimeException("Failed to encipher value", e);
        }
    }
}
