package br.com.wdc.framework.cube.remote;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.security.RSA;

/**
 * Security configuration for a remote application module.
 * <p>
 * Each application module has its own instance — no shared global state.
 * Provides RSA cipher (for client↔server key exchange) and RSA signing
 * (for session ID and history fragment validation).
 */
public final class RemoteAppSecurity {

    private static final String SHA256_WITH_RSA = "SHA256withRSA";
    private static final Log LOG = Log.getLogger(RemoteAppSecurity.class);

    private final RSA rsa;
    private final String webKey;

    private PublicKey signPublicKey;
    private PrivateKey signPrivateKey;

    private RemoteAppSecurity(RSA rsa, String webKey, String signPublicKeyB64, String signPrivateKeyB64) {
        this.rsa = rsa;
        this.webKey = webKey;
        try {
            prepareSignKeys(signPublicKeyB64, signPrivateKeyB64);
        } catch (Exception e) {
            LOG.error("Sign keys not initialized", e);
        }
    }

    /**
     * Creates a security instance from system properties.
     *
     * @param cipherPublicKeyProp  property name for RSA cipher public key (format: "exponent:modulus" in base36)
     * @param cipherPrivateKeyProp property name for RSA cipher private key (base36)
     * @param signPublicKeyProp    property name for RSA sign public key (URL-safe Base64)
     * @param signPrivateKeyProp   property name for RSA sign private key (URL-safe Base64)
     */
    public static RemoteAppSecurity fromSystemProperties(
            String cipherPublicKeyProp,
            String cipherPrivateKeyProp,
            String signPublicKeyProp,
            String signPrivateKeyProp) {

        String sPublicExponent;
        String sPublicKey;
        String sPrivateKey;

        var wdcPublicKey = System.getProperty(cipherPublicKeyProp);
        var wdcPrivateKey = System.getProperty(cipherPrivateKeyProp);
        if (StringUtils.isBlank(wdcPublicKey) || StringUtils.isBlank(wdcPrivateKey)) {
            throw new IllegalStateException(
                    "Remote security keys not configured. Set '" + cipherPublicKeyProp + "' and '"
                    + cipherPrivateKeyProp + "' (via system properties or application.local.toml [remote] section).");
        }
        var parts = StringUtils.split(wdcPublicKey, ":");
        sPublicExponent = parts[0];
        sPublicKey = parts[1];
        sPrivateKey = wdcPrivateKey;

        var publicExponent = new BigInteger(sPublicExponent, 36);
        var publicKey = new BigInteger(sPublicKey, 36);
        var privateKey = new BigInteger(sPrivateKey, 36);
        var rsa = new RSA(publicExponent, privateKey, publicKey);

        var signPub = System.getProperty(signPublicKeyProp);
        var signPrv = System.getProperty(signPrivateKeyProp);
        if (StringUtils.isBlank(signPub) || StringUtils.isBlank(signPrv)) {
            throw new IllegalStateException(
                    "Remote sign keys not configured. Set '" + signPublicKeyProp + "' and '"
                    + signPrivateKeyProp + "' (via system properties or application.local.toml [remote] section).");
        }

        return new RemoteAppSecurity(rsa, wdcPublicKey, signPub, signPrv);
    }

    /**
     * Creates a security instance with development-mode default keys.
     */
    public static RemoteAppSecurity createDefault() {
        return fromSystemProperties(
                "wdc.web.public_key",
                "wdc.web.private_key",
                "wdc.sign.wdc.web.public_key",
                "wdc.sign.wdc.web.private_key");
    }

    public String getWebKey() {
        return webKey;
    }

    public RSA getRsa() {
        return rsa;
    }

    public byte[] sign(byte[] contentBytes) {
        try {
            var sign = Signature.getInstance(SHA256_WITH_RSA);
            sign.initSign(signPrivateKey);
            sign.update(contentBytes);
            return sign.sign();
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
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
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    public boolean isSignatureValid(byte[] contentBytes, byte[] signature) {
        try {
            var verify = Signature.getInstance(SHA256_WITH_RSA);
            verify.initVerify(signPublicKey);
            verify.update(contentBytes);
            return verify.verify(signature);
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    private void prepareSignKeys(String publicKeyInBase64, String privateKeyInBase64)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        var b64Dec = Base64.getUrlDecoder();

        byte[] publicBytes = b64Dec.decode(publicKeyInBase64);
        var keySpecPublic = new X509EncodedKeySpec(publicBytes);
        var keyFactoryPublic = KeyFactory.getInstance("RSA");
        this.signPublicKey = keyFactoryPublic.generatePublic(keySpecPublic);

        byte[] privateBytes = b64Dec.decode(privateKeyInBase64);
        var keySpecPrivate = new PKCS8EncodedKeySpec(privateBytes);
        var keyFactoryPrivate = KeyFactory.getInstance("RSA");
        this.signPrivateKey = keyFactoryPrivate.generatePrivate(keySpecPrivate);
    }
}
