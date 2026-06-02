package br.com.wdc.framework.cube.remote.bridge.teavm.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.crypto.Crypto;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * Web Crypto API abstraction for browser environment (TeaVM).
 * <p>
 * Provides: random bytes generation via {@code crypto.getRandomValues},
 * PBKDF2 key derivation, and AES-GCM encryption.
 */
public final class WebCrypto {

    private WebCrypto() {
    }

    /**
     * Generates cryptographically secure random bytes.
     */
    public static byte[] getRandomBytes(int n) {
        var array = new Uint8Array(n);
        Crypto.current().getRandomValues(array);
        var bytes = new byte[n];
        for (int i = 0; i < n; i++) {
            bytes[i] = (byte) array.get(i);
        }
        return bytes;
    }

    /**
     * Converts a Java byte[] to a JS Uint8Array.
     */
    public static Uint8Array toUint8Array(byte[] bytes) {
        var array = new Uint8Array(bytes.length);
        for (int i = 0; i < bytes.length; i++) {
            array.set(i, (short) (bytes[i] & 0xFF));
        }
        return array;
    }

    /**
     * Derives an AES-GCM 256-bit key from a password using PBKDF2 (SHA-256, 250k iterations).
     * Calls back with (CryptoKey, iv) on success.
     */
    @JSBody(params = {"pwd", "salt", "iv", "callback"}, script = ""
            + "var pwdBuf = new TextEncoder().encode(pwd);"
            + "crypto.subtle.importKey('raw', pwdBuf, { name: 'PBKDF2' }, false, ['deriveKey'])"
            + ".then(function(keyMaterial) {"
            + "  return crypto.subtle.deriveKey("
            + "    { name: 'PBKDF2', salt: salt, iterations: 250000, hash: 'SHA-256' },"
            + "    keyMaterial,"
            + "    { name: 'AES-GCM', length: 256 },"
            + "    false,"
            + "    ['encrypt', 'decrypt']"
            + "  );"
            + "})"
            + ".then(function(aesKey) {"
            + "  callback(aesKey, iv);"
            + "})"
            + ".catch(function(e) {"
            + "  console.error('[WebCrypto] AES key derivation failed:', e);"
            + "});")
    public static native void deriveAesKey(String pwd, Uint8Array salt, Uint8Array iv, JsBiObjectConsumer<Uint8Array> callback);

    /**
     * Encrypts text with AES-GCM using the given key and IV.
     * Calls back with base64-encoded ciphertext, or empty string on failure.
     */
    @JSBody(params = {"text", "key", "iv", "callback"}, script = ""
            + "var encoded = new TextEncoder().encode(text);"
            + "crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, encoded)"
            + ".then(function(ciphertext) {"
            + "  var bytes = new Uint8Array(ciphertext);"
            + "  var binary = '';"
            + "  for (var i = 0; i < bytes.length; i++) binary += String.fromCodePoint(bytes[i]);"
            + "  callback(btoa(binary));"
            + "})"
            + ".catch(function(e) {"
            + "  console.error('[WebCrypto] Encryption failed:', e);"
            + "  callback('');"
            + "});")
    public static native void encrypt(String text, JSObject key, JSObject iv, JsStringConsumer callback);
}
