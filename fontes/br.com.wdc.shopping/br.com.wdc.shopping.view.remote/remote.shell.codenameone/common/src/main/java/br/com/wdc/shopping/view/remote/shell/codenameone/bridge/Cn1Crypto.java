package br.com.wdc.shopping.view.remote.shell.codenameone.bridge;

import com.codename1.security.Cipher;
import com.codename1.security.Hmac;
import com.codename1.security.SecretKey;
import com.codename1.security.SecureRandom;
import com.codename1.util.Base64;
import com.codename1.util.BigInteger;

/**
 * Handshake do bridge no Codename One — RSA (textbook, base36) + PBKDF2-HMAC-SHA256 + AES-256-GCM.
 *
 * <p>
 * Reproduz, com APIs nativas do CN1, exatamente o {@code ClientCrypto}/{@code SecretContext} do
 * bridge Java (e o {@code DataSecurity} do Flutter). Receita validada ponta-a-ponta por um probe
 * Java puro com login real (admin/admin) contra o servidor.
 * </p>
 *
 * <ul>
 * <li>{@link #signature()} — valor do campo {@code "secret"} da 1ª mensagem WS;</li>
 * <li>{@link #encipher(String)} — cifra um valor (ex.: {@code p.password}) com a chave AES da sessão.</li>
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
     * Gera o segredo da sessão a partir da chave pública RSA do servidor
     * ({@code appSKey} = {@code "expoente:modulo"} em base36, vinda do {@code /api/session/init}).
     */
    public static Cn1Crypto generate(String appSKey) {
        int colon = appSKey.indexOf(':');
        BigInteger publicExponent = new BigInteger(appSKey.substring(0, colon), 36);
        BigInteger publicModulus = new BigInteger(appSKey.substring(colon + 1), 36);

        // senha aleatória: 12 bytes -> base64url sem padding
        byte[] randomBytes = SecureRandom.bytes(12);
        String pwd = stripPadding(Base64.encodeUrlSafe(randomBytes));
        byte[] pwdBytes = utf8(pwd);

        byte[] salt = SecureRandom.bytes(16);
        byte[] iv = SecureRandom.bytes(12);

        // AES-256 via PBKDF2-HMAC-SHA256
        byte[] aesKey = pbkdf2(pwdBytes, salt, PBKDF2_ITERATIONS, AES_KEY_BYTES);

        // RSA textbook: encripta os bytes do base64(pwdBytes); base36
        String pwdBase64 = Base64.encodeNoNewline(pwdBytes); // base64 padrão (com padding)
        BigInteger message = new BigInteger(1, utf8(pwdBase64)); // positivo (unsigned)
        BigInteger encrypted = message.modPow(publicExponent, publicModulus);

        String signature = encrypted.toString(36)
                + "." + stripPadding(Base64.encodeUrlSafe(salt))
                + "." + stripPadding(Base64.encodeUrlSafe(iv));

        return new Cn1Crypto(signature, aesKey, iv);
    }

    /** Cifra um texto com AES-256-GCM (tag 128) da sessão e devolve base64 padrão. */
    public String encipher(String plaintext) {
        byte[] out = Cipher.aesEncrypt(Cipher.AES_GCM, new SecretKey("AES", aesKey), iv, new byte[0], utf8(plaintext));
        return Base64.encodeNoNewline(out);
    }

    // -- PBKDF2-HMAC-SHA256 (dkLen == 32 == 1 bloco HMAC-SHA256) --

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
