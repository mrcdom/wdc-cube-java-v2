package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.math.BigInteger;
import java.util.Base64;

import org.teavm.jso.JSObject;
import org.teavm.jso.browser.Storage;
import org.teavm.jso.typedarrays.Uint8Array;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Console;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Cookies;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.WebCrypto;

/**
 * Security boot sequence for the remote bridge (TeaVM).
 * <p>
 * Reads the {@code app_skey} cookie (RSA public key in base36), generates a random AES password, encrypts it with RSA,
 * derives an AES-GCM key via PBKDF2, and stores everything for later use by {@link DataSecurity}.
 * <p>
 * This replaces the former {@code security-boot.js} script.
 */
public final class SecurityBoot {

    private static String signature;
    private static Uint8Array aesKey;
    private static JSObject aesIv;
    private static boolean ready = false;

    private SecurityBoot() {
    }

    /**
     * Returns the app_signature string (sent on first WS frame).
     */
    public static String getSignature() {
        return signature;
    }

    /**
     * Returns true once AES key derivation is complete.
     */
    public static boolean isReady() {
        return ready;
    }

    /**
     * Must be called at application startup, before WebSocket connection. Reads app_skey cookie, performs RSA
     * encryption, sets app_signature cookie, and begins async AES key derivation.
     */
    public static void initialize() {
        var appSKey = Cookies.get("app_skey");
        if (appSKey == null || appSKey.isEmpty()) {
            Console.warn("[SecurityBoot] No app_skey cookie found");
            transferAppId();
            return;
        }

        try {
            var parts = appSKey.split(":");
            var exponent = new BigInteger(parts[0], 36);
            var modulus = new BigInteger(parts[1], 36);

            // Generate random password: base64url of 12 random bytes
            var rndBytes = WebCrypto.getRandomBytes(12);
            var pwd = base64UrlEncode(rndBytes);
            var pwdBytes = pwd.getBytes();

            // RSA encrypt: message → base64 → UTF-8 bytes → BigInt → modPow → base36
            var messageBase64 = base64Encode(pwdBytes);
            var safeBytes = messageBase64.getBytes();
            var messageBigInt = new BigInteger(1, safeBytes);
            var encrypted = messageBigInt.modPow(exponent, modulus);
            var cryptedPwd = encrypted.toString(36);

            // Generate salt (16 bytes) and IV (12 bytes)
            var salt = WebCrypto.getRandomBytes(16);
            var iv = WebCrypto.getRandomBytes(12);

            // Build signature
            signature = cryptedPwd + "." + base64UrlEncode(salt) + "." + base64UrlEncode(iv);

            // Set app_signature cookie and remove app_skey
            Cookies.set("app_signature", signature, "/");
            Cookies.remove("app_skey");

            Console.log("[SecurityBoot] app_signature cookie set");

            // Derive AES-GCM key asynchronously
            WebCrypto.deriveAesKey(pwd, WebCrypto.toUint8Array(salt), WebCrypto.toUint8Array(iv),
                    SecurityBoot::onAesKeyReady);

        } catch (Exception e) {
            Console.error("[SecurityBoot] Failed: " + e.getMessage());
        }

        transferAppId();
    }

    /**
     * Encrypts text with AES-GCM using the derived key. Calls back with base64-encoded ciphertext, or empty string on
     * failure.
     */
    public static void cipher(String text, JsStringConsumer callback) {
        if (!ready || aesKey == null || aesIv == null) {
            Console.error("[SecurityBoot] AES key not yet available");
            callback.accept("");
            return;
        }
        WebCrypto.encrypt(text, aesKey, aesIv, callback);
    }

    // -- Private helpers --

    private static void transferAppId() {
        var appId = Cookies.get("app_id");
        if (appId != null && !appId.isEmpty()) {
            Cookies.remove("app_id");
            var existing = Storage.getSessionStorage().getItem("app_id");
            if (existing == null || existing.isEmpty()) {
                Storage.getSessionStorage().setItem("app_id", appId);
                Console.log("[SecurityBoot] app_id stored: " + appId);
            }
        }
    }

    private static void onAesKeyReady(Uint8Array key, Uint8Array iv) {
        aesKey = key;
        aesIv = iv;
        ready = true;
        Console.log("[SecurityBoot] AES-GCM key derived");
    }

    private static String base64UrlEncode(byte[] buf) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String base64Encode(byte[] buf) {
        return Base64.getEncoder().encodeToString(buf);
    }
}
