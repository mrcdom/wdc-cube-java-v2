package br.com.wdc.shopping.view.vaadin.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.codec.Base62;

public class IntentSigner {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final String SIGN_PARAM = "sign";

    private final byte[] secret;

    public IntentSigner() {
        this.secret = new byte[32];
        new SecureRandom().nextBytes(this.secret);
    }

    public String sign(String intentStr) {
        var signature = computeSignature(intentStr);
        if (intentStr.contains("?")) {
            return intentStr + "&" + SIGN_PARAM + "=" + signature;
        } else {
            return intentStr + "?" + SIGN_PARAM + "=" + signature;
        }
    }

    public boolean verify(String signedIntentStr) {
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

    public String stripSignature(String signedIntentStr) {
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

    public String extractSignature(String signedIntentStr) {
        var signIdx = signedIntentStr.lastIndexOf(SIGN_PARAM + "=");
        if (signIdx < 0) {
            return null;
        }
        return signedIntentStr.substring(signIdx + SIGN_PARAM.length() + 1);
    }

    private String computeSignature(String data) {
        try {
            var mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(secret, HMAC_SHA256));
            var hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base62.BEAN.encodeToString(hash);
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
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
