package br.com.wdc.shopping.view.react.skeleton.util;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

public class DataSecurity {

    private final AppSecurity security;
    private final SecretKeyFactory keyFactory;

    private SecretKeySpec secret;
    private byte[] iv;

    public DataSecurity() {
        try {
            security = AppSecurity.BEAN;
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    public void updateSecret(String signature) {
        try {
            var b64 = Base64.getUrlDecoder();
            var signatureParts = signature.split("\\.");

            String password;
            {
                var messageEncryptedAsBigInt = new BigInteger(signatureParts[0], 36);
                var messageAsBigint = security.getRsa().decrypt(messageEncryptedAsBigInt);
                var messageAsSafeBytes = messageAsBigint.toByteArray();
                var message = Base64.getDecoder().decode(messageAsSafeBytes);

                password = new String(message, StandardCharsets.UTF_8);
            }

            var salt = b64.decode(signatureParts[1]);
            this.iv = b64.decode(signatureParts[2]);

            var spec = new PBEKeySpec(password.toCharArray(), salt, 250000, 256);
            var secretKey = keyFactory.generateSecret(spec);
            this.secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
        } catch (Exception e) {
            var logger = LoggerFactory.getLogger(this.getClass());
            logger.error("updateSecret", e);
        }
    }

    // :: Binary Cypher

    public byte[] cipher(byte[] binData) {
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            var gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secret, gcmParameterSpec);
            return cipher.doFinal(binData);
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    public byte[] decipher(byte[] binData) {
        try {
            var cipher = Cipher.getInstance("AES/GCM/NoPadding");
            var gcmParameterSpec = new GCMParameterSpec(128, iv);
            cipher.init(Cipher.DECRYPT_MODE, secret, gcmParameterSpec);
            return cipher.doFinal(binData);
        } catch (Exception e) {
            throw ExceptionUtils.asRuntimeException(e);
        }
    }

    public String b64Cipher(String text) {
        var textBytes = text.getBytes(StandardCharsets.UTF_8);
        var cipheredTextBytes = this.cipher(textBytes);
        return Base64.getEncoder().encodeToString(cipheredTextBytes);
    }

    public String b64Decipher(String b64Text) {
        var cipheredTextBytes = Base64.getDecoder().decode(b64Text);
        var textBytes = this.decipher(cipheredTextBytes);
        return new String(textBytes, StandardCharsets.UTF_8);
    }

}