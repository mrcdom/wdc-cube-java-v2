package br.com.wdc.shopping.view.teavm;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.shopping.domain.security.CryptoProvider;

/**
 * Assina e verifica fragments de URL usando HMAC-SHA256 truncado + Base62.
 * <p>
 * A assinatura impede modificação casual do hash da URL pelo usuário.
 * Usa um secret aleatório por sessão (gerado via crypto.getRandomValues no browser).
 * <p>
 * Formato: {@code intent&s=<base62_signature>} (ex: {@code home?userId=0&s=3kTm8Gx2Rn1})
 */
class IntentSigner {

    private static final String SIGN_PARAM = "s";
    private static final int TRUNCATE_BYTES = 8; // 8 bytes → ~11 chars Base62 (2^64 combinations)

    private final byte[] secret;

    IntentSigner(byte[] secret) {
        this.secret = secret;
    }

    String sign(String intentStr) {
        var signature = computeSignature(intentStr);
        if (intentStr.contains("?")) {
            return intentStr + "&" + SIGN_PARAM + "=" + signature;
        } else {
            return intentStr + "?" + SIGN_PARAM + "=" + signature;
        }
    }

    boolean verify(String signedIntentStr) {
        var signIdx = signedIntentStr.lastIndexOf(SIGN_PARAM + "=");
        if (signIdx < 0) {
            return false;
        }

        var actualSignature = signedIntentStr.substring(signIdx + SIGN_PARAM.length() + 1);

        var separatorIdx = signIdx - 1;
        if (separatorIdx < 0) {
            return false;
        }
        var originalIntent = signedIntentStr.substring(0, separatorIdx);

        var expectedSignature = computeSignature(originalIntent);
        return constantTimeEquals(expectedSignature, actualSignature);
    }

    String stripSignature(String signedIntentStr) {
        var signIdx = signedIntentStr.lastIndexOf(SIGN_PARAM + "=");
        if (signIdx < 0) {
            return signedIntentStr;
        }
        var separatorIdx = signIdx - 1;
        if (separatorIdx < 0) {
            return signedIntentStr;
        }
        return signedIntentStr.substring(0, separatorIdx);
    }

    private String computeSignature(String data) {
        var crypto = CryptoProvider.BEAN.get();
        var fullHmac = crypto.hmacSha256(secret, data.getBytes(StandardCharsets.UTF_8));
        var truncated = Arrays.copyOf(fullHmac, TRUNCATE_BYTES);
        return Base62.BEAN.encodeToString(truncated);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
