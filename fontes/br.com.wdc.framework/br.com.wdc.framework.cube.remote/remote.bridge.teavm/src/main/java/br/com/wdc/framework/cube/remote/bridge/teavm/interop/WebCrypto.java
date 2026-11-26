package br.com.wdc.framework.cube.remote.bridge.teavm.interop;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSFunctor;
import org.teavm.jso.JSObject;
import org.teavm.jso.crypto.Crypto;
import org.teavm.jso.typedarrays.Uint8Array;

/**
 * The cryptography of remote protocol version 1 in the browser (TeaVM): random bytes, the per-connection key
 * agreement and the ciphered values.
 *
 * <ul>
 * <li><b>Key pair</b>: an ephemeral ECDH P-256 key pair; its public key goes out as Base64 of the X.509
 * SubjectPublicKeyInfo ({@link #newKeyPair}).</li>
 * <li><b>Connection key</b>: HKDF-SHA256 over the 32-byte ECDH x-coordinate, salt = UTF-8 session id, info =
 * {@value #KEY_INFO}, 32 bytes, used as an AES-256-GCM key ({@link #deriveKey}).</li>
 * <li><b>Ciphered value</b>: AES-256-GCM, 128-bit tag, a random 12-byte IV of its own, written as
 * {@code Base64(IV ‖ ciphertext ‖ tag)} ({@link #cipher}, {@link #decipher}).</li>
 * </ul>
 * <p>
 * {@code crypto.subtle} exists only in secure contexts (HTTPS or localhost). Elsewhere (plain HTTP, e.g. QA reached
 * by IP) the same primitives come from {@code wdcCryptoFallback}, a bundle of @noble the page loads from
 * {@code js/crypto-compat.js}, with identical parameters. A key pair records which path made it ({@code fallback}),
 * and the key derived from it follows: a {@code CryptoKey} on the native path, a raw {@code Uint8Array} on the
 * fallback path; {@link #cipher} and {@link #decipher} accept either.
 */
public final class WebCrypto {

    /** The HKDF info that names what the derived key is for. */
    public static final String KEY_INFO = "wdc-cube-remote/aes-256-gcm/v1";

    /** Receives a new key pair and its public key (Base64 SubjectPublicKeyInfo). */
    @JSFunctor
    @FunctionalInterface
    public interface KeyPairConsumer extends JSObject {
        void accept(JSObject keyPair, String publicKey);
    }

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
     * A new ephemeral ECDH P-256 key pair. Calls {@code onPair} with the pair (opaque, for {@link #deriveKey}) and its
     * public key as Base64 of the X.509 SubjectPublicKeyInfo, or {@code onError} with the reason.
     */
    @JSBody(params = { "onPair", "onError" }, script = """
            function b64(bytes) {
                var binary = '';
                for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
                return btoa(binary);
            }
            if (!crypto.subtle) {
                if (typeof wdcCryptoFallback === 'undefined') {
                    onError('crypto.subtle unavailable and wdcCryptoFallback not loaded');
                    return;
                }
                try {
                    var pair = wdcCryptoFallback.ecdhKeyPair();
                    onPair({ fallback: true, privateKey: pair.privateKey }, b64(pair.publicKey));
                } catch (e) {
                    onError(String(e));
                }
                return;
            }
            crypto.subtle.generateKey({ name: 'ECDH', namedCurve: 'P-256' }, false, ['deriveBits'])
            .then(function(pair) {
                return crypto.subtle.exportKey('spki', pair.publicKey).then(function(spki) {
                    onPair({ fallback: false, privateKey: pair.privateKey }, b64(new Uint8Array(spki)));
                });
            })
            .catch(function(e) {
                onError(String(e));
            });
            """)
    public static native void newKeyPair(KeyPairConsumer onPair, JsStringConsumer onError);

    /**
     * Derives the connection key from the own key pair and the peer's public key (Base64 SubjectPublicKeyInfo), with
     * the session id as salt. Calls {@code onKey} with the key, or {@code onError} with the reason (e.g. the peer key
     * is not a P-256 public key).
     */
    public static void deriveKey(JSObject keyPair, String peerKey, String sessionId, JsObjectConsumer onKey,
            JsStringConsumer onError) {
        deriveKey(keyPair, peerKey, sessionId, KEY_INFO, onKey, onError);
    }

    @JSBody(params = { "keyPair", "peerKey", "sessionId", "keyInfo", "onKey", "onError" }, script = """
            var peer;
            try {
                var binary = atob(peerKey);
                peer = new Uint8Array(binary.length);
                for (var i = 0; i < binary.length; i++) peer[i] = binary.charCodeAt(i);
            } catch (e) {
                onError('the peer key is not Base64');
                return;
            }
            var salt = new TextEncoder().encode(sessionId);
            var info = new TextEncoder().encode(keyInfo);
            if (keyPair.fallback) {
                try {
                    var shared = wdcCryptoFallback.ecdhSharedSecret(keyPair.privateKey, peer);
                    var key = wdcCryptoFallback.hkdfSha256(shared, salt, info, 32);
                    shared.fill(0);
                    onKey(key);
                } catch (e) {
                    onError(String(e));
                }
                return;
            }
            var subtle = crypto.subtle;
            subtle.importKey('spki', peer, { name: 'ECDH', namedCurve: 'P-256' }, false, [])
            .then(function(peerPublic) {
                return subtle.deriveBits({ name: 'ECDH', public: peerPublic }, keyPair.privateKey, 256);
            })
            .then(function(shared) {
                return subtle.importKey('raw', shared, 'HKDF', false, ['deriveKey']);
            })
            .then(function(ikm) {
                return subtle.deriveKey({ name: 'HKDF', hash: 'SHA-256', salt: salt, info: info }, ikm,
                        { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
            })
            .then(function(key) {
                onKey(key);
            })
            .catch(function(e) {
                onError(String(e));
            });
            """)
    private static native void deriveKey(JSObject keyPair, String peerKey, String sessionId, String keyInfo,
            JsObjectConsumer onKey, JsStringConsumer onError);

    /**
     * Ciphers a text with the connection key: {@code Base64(IV ‖ ciphertext ‖ tag)}, with a random IV of its own.
     * Calls back with the value, or an empty string on failure.
     */
    @JSBody(params = { "key", "text", "callback" }, script = """
            function b64(bytes) {
                var binary = '';
                for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
                return btoa(binary);
            }
            function seal(iv, sealed) {
                var out = new Uint8Array(12 + sealed.length);
                out.set(iv, 0);
                out.set(sealed, 12);
                return b64(out);
            }
            var iv = crypto.getRandomValues(new Uint8Array(12));
            var plain = new TextEncoder().encode(text);
            if (key instanceof Uint8Array) {
                try {
                    callback(seal(iv, wdcCryptoFallback.aesGcmEncrypt(key, iv, plain)));
                } catch (e) {
                    console.error('[WebCrypto] cipher failed:', e);
                    callback('');
                }
                return;
            }
            crypto.subtle.encrypt({ name: 'AES-GCM', iv: iv }, key, plain)
            .then(function(sealed) {
                callback(seal(iv, new Uint8Array(sealed)));
            })
            .catch(function(e) {
                console.error('[WebCrypto] cipher failed:', e);
                callback('');
            });
            """)
    public static native void cipher(JSObject key, String text, JsStringConsumer callback);

    /**
     * Deciphers a value made by {@link #cipher} (here or at the host). Calls back with the text, or an empty string
     * when the value is malformed or does not authenticate (tampered, or ciphered with another connection's key).
     */
    @JSBody(params = { "key", "value", "callback" }, script = """
            var bytes;
            try {
                var binary = atob(value);
                bytes = new Uint8Array(binary.length);
                for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            } catch (e) {
                callback('');
                return;
            }
            if (bytes.length < 12 + 16) {
                callback('');
                return;
            }
            var iv = bytes.slice(0, 12);
            var sealed = bytes.slice(12);
            if (key instanceof Uint8Array) {
                try {
                    callback(wdcCryptoFallback.aesGcmDecrypt(key, iv, sealed));
                } catch (e) {
                    console.warn('[WebCrypto] a ciphered value was refused:', e);
                    callback('');
                }
                return;
            }
            crypto.subtle.decrypt({ name: 'AES-GCM', iv: iv }, key, sealed)
            .then(function(plain) {
                callback(new TextDecoder().decode(new Uint8Array(plain)));
            })
            .catch(function(e) {
                console.warn('[WebCrypto] a ciphered value was refused:', e);
                callback('');
            });
            """)
    public static native void decipher(JSObject key, String value, JsStringConsumer callback);
}
