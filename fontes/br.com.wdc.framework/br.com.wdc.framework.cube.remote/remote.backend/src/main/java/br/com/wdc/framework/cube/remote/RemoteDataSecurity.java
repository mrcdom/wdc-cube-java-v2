package br.com.wdc.framework.cube.remote;

import br.com.wdc.framework.commons.security.ChannelCrypto;

/**
 * The key of a session's current connection, and the ciphering of the sensitive values that travel on it.
 *
 * <p>
 * Each connection agrees on its own key with the client ({@code DispatcherController}, the handshake of protocol 1);
 * a reconnection agrees on a new one and replaces it here. Every value is ciphered with an IV of its own
 * ({@link ChannelCrypto#cipher}).
 * </p>
 */
public class RemoteDataSecurity {

    /** The request key under which the dispatcher hands the connection's key to a session being created. */
    public static final String CHANNEL_KEY = "wdc.channelKey";

    private volatile byte[] key;

    /** The key agreed by the current connection. */
    public void useKey(byte[] key) {
        this.key = key == null ? null : key.clone();
    }

    /** Whether a connection has agreed on a key. */
    public boolean hasKey() {
        return key != null;
    }

    /**
     * Ciphers a text for the client.
     *
     * @throws IllegalStateException without an agreed key
     */
    public String b64Cipher(String text) {
        return ChannelCrypto.cipher(requireKey(), text);
    }

    /**
     * Deciphers a value the client ciphered.
     *
     * @throws IllegalStateException    without an agreed key
     * @throws IllegalArgumentException if the value does not authenticate with the key
     */
    public String b64Decipher(String value) {
        return ChannelCrypto.decipher(requireKey(), value);
    }

    private byte[] requireKey() {
        var current = key;
        if (current == null) {
            throw new IllegalStateException("No key agreed on this session's connection");
        }
        return current;
    }
}
