package br.com.wdc.framework.cube.remote.bridge.java.model;

import br.com.wdc.framework.commons.security.ChannelCrypto;

/**
 * The key this client and the host agreed on for the current connection (ECDH P-256, HKDF-SHA256).
 * <p>
 * Ciphers the sensitive values that travel on the channel — a typed password ({@code p.password}), the storage the
 * client restores — and deciphers the ones the host sends. Every value carries an IV of its own.
 */
public final class ChannelKey {

    private final byte[] key;

    public ChannelKey(byte[] key) {
        this.key = key.clone();
    }

    /** Ciphers a value for the host, which deciphers it with {@code app.b64Decipher()}. */
    public String encipher(String plaintext) {
        return ChannelCrypto.cipher(key, plaintext);
    }

    /** Deciphers a value the host ciphered. */
    public String decipher(String ciphered) {
        return ChannelCrypto.decipher(key, ciphered);
    }
}
