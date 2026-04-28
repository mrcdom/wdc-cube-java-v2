package br.com.wdc.shopping.view.react.skeleton.util;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.commons.security.RSA;

public enum AppSecurity {
    BEAN;

    private static final String SHA256_WITH_RSA = "SHA256withRSA";

    private RSA rsa;
    private String webKey;

    private PublicKey signPublicKey;
    private PrivateKey signPrivateKey;

    AppSecurity() {
        Logger logger = LoggerFactory.getLogger(AppSecurity.class);

        { // Cipher RSA
            String sPublicKey;
            String sPublicExponent;

            var wdcPublicKey = System.getProperty("wdc.web.public_key");
            var wdcPrivateKey = System.getProperty("wdc.web.private_key");
            if (StringUtils.isBlank(wdcPublicKey) || StringUtils.isBlank(wdcPrivateKey)) {
                // Fallback : It is util for development porpose
                sPublicExponent = "1ekh";
                sPublicKey = "3n88eu224huxfvj7lndkkf4n2vye4lus611fecnoc57qod2m7d";
                wdcPrivateKey = "2n9arhz94hevkz4ge8vxwje5c37k7aqol1st01wvzln81u5m69";
                wdcPublicKey = sPublicExponent + ":" + sPublicKey;
            } else {
                var parts = StringUtils.split(wdcPublicKey, ":");
                sPublicExponent = parts[0];
                sPublicKey = parts[1];
            }

            var publicExponent = new BigInteger(sPublicExponent, 36);
            var publicKey = new BigInteger(sPublicKey, 36);
            var privateKey = new BigInteger(wdcPrivateKey, 36);
            this.rsa = new RSA(publicExponent, privateKey, publicKey);
            this.webKey = wdcPublicKey;
        }

        // Initialize signature resources
        try {
            var pk = System.getProperty("wdc.sign.wdc.web.public_key");
            var pv = System.getProperty("wdc.sign.wdc.web.private_key");
            if (StringUtils.isBlank(pk) || StringUtils.isBlank(pk)) {
                // Fallback : It is util for development porpose
                pk = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAIkDxriJZ2BLyg26A7hR-qzJPRSj33156sXy_r6JLa0NWz2uY1z9FwnQRtrU3CztutyAIhwyHaOxfMGWyvgFsokCAwEAAQ==";
                pv = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAiQPGuIlnYEvKDboDuFH6rMk9FKPffXnqxfL-voktrQ1bPa5jXP0XCdBG2tTcLO263IAiHDIdo7F8wZbK-AWyiQIDAQABAkABpau1PygHILu4tTC0ZEsblbnhltdHxfPW2m_KGUVqXjg71xASB-0rctP7pu9qgOPaj_ltTki3xHXQX07QKnJZAiEAvxFzS6c6FqJ8LbrVta72W5i-pb3AkLAM-wyoPmAOOxsCIQC3k8lagaTvRvdlkLrfJZ3K4q4JcsUHG6M2h43P34SfKwIgYtC9ljTIYAhsvKHSAQKZusmGX-WA_9NtAzGKmafH9F0CIGVwnpUKio9F0bMn1Hs2GAliVPUXnFQfK4MYSH6Tbn9dAiEAimwgt_xSziP2RejiFY3_Ek6ROpRG6uL9s89NuaoGFvY=";
            }

            this.prepareSignKeys(pk, pv);
        } catch (Exception exn) {
            logger.error("Sign not initialized", exn);
        }
    }

    public String getWebKey() {
        return webKey;
    }

    public RSA getRsa() {
        return this.rsa;
    }

    private void prepareSignKeys(String publicKeyInBase64, String privateKeyInbase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        var b64Dec = Base64.getUrlDecoder();

        // Decodificar as chaves
        byte[] publicBytes = b64Dec.decode(publicKeyInBase64);
        var keySpecPublic = new X509EncodedKeySpec(publicBytes);
        var keyFactoryPublic = KeyFactory.getInstance("RSA");
        this.signPublicKey = keyFactoryPublic.generatePublic(keySpecPublic);

        byte[] privateBytes = b64Dec.decode(privateKeyInbase64);
        var keySpecPrivate = new PKCS8EncodedKeySpec(privateBytes);
        var keyFactoryPrivate = KeyFactory.getInstance("RSA");
        this.signPrivateKey = keyFactoryPrivate.generatePrivate(keySpecPrivate);
    }

    public byte[] sign(byte[] contentBytes) {
        try {
            var sign = Signature.getInstance(SHA256_WITH_RSA);
            sign.initSign(signPrivateKey);
            sign.update(contentBytes);
            return sign.sign();
        } catch (Exception exn) {
            throw ExceptionUtils.asRuntimeException(exn);
        }
    }

    public byte[] signAsHash(byte[] contentBytes) {
        try {
            var sign = Signature.getInstance(SHA256_WITH_RSA);
            sign.initSign(signPrivateKey);
            sign.update(contentBytes);
            var signature = sign.sign();

            MessageDigest md5 = DigestUtils.getMd5Digest();
            return md5.digest(signature);
        } catch (Exception exn) {
            throw ExceptionUtils.asRuntimeException(exn);
        }
    }

    public boolean isThisSignatureValid(byte[] contentBytes, byte[] signature) {
        try {
            var verify = Signature.getInstance(SHA256_WITH_RSA);
            verify.initVerify(signPublicKey);
            verify.update(contentBytes);
            return verify.verify(signature);
        } catch (Exception exn) {
            throw ExceptionUtils.asRuntimeException(exn);
        }
    }

    @SuppressWarnings("java:S4426")
    public static void main(String[] args) throws NoSuchAlgorithmException {
        var b64Enc = Base64.getUrlEncoder();

        @SuppressWarnings("java:S106")
        var out = System.out;

        // :: Exemplo de como gerar chaves para a cifragem
        {
            var rsa = new RSA(256, new SecureRandom());

            var pk = rsa.getPublicExponent().toString(36) + ":" + rsa.getPublicKey().toString(36);
            var pv = rsa.getPublicKey().toString(36);

            out.println("<property name=\"wdc.web.public_key\" value=\"" + pk + "\"/>");
            out.println("<property name=\"wdc.web.private_key\" value=\"" + pv + "\"/>");
        }

        out.println();

        // :: Exemplo de como gerar as Chaves
        {
            var keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(512);
            var pair = keyGen.generateKeyPair();
            var publicKey = pair.getPublic();
            var privateKey = pair.getPrivate();

            var pk = b64Enc.encodeToString(publicKey.getEncoded());
            var pv = b64Enc.encodeToString(privateKey.getEncoded());

            out.println("<property name=\"wdc.sign.wdc.web.public_key\" value=\"" + pk + "\"/>");
            out.println("<property name=\"wdc.sign.wdc.web.private_key\" value=\"" + pv + "\"/>");
        }

    }
}
