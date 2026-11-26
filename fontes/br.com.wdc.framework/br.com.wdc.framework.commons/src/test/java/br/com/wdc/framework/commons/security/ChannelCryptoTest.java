package br.com.wdc.framework.commons.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.util.Base64;
import java.util.HexFormat;

import org.junit.Test;

/** The remote channel's cryptography: HKDF against RFC 5869, the key agreement, and the ciphered values. */
public class ChannelCryptoTest {

    private static final HexFormat HEX = HexFormat.of();

    @Test
    public void hkdfMatchesRfc5869TestCase1() {
        var okm = ChannelCrypto.hkdfSha256(
                HEX.parseHex("0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b0b"),
                HEX.parseHex("000102030405060708090a0b0c"),
                HEX.parseHex("f0f1f2f3f4f5f6f7f8f9"), 42);

        assertEquals("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865",
                HEX.formatHex(okm));
    }

    @Test
    public void bothEndsDeriveTheSameKey_fromEachOthersPublicKey() {
        var client = ChannelCrypto.newKeyPair();
        var host = ChannelCrypto.newKeyPair();
        var clientKey = ChannelCrypto.encodePublicKey(client.getPublic());
        var hostKey = ChannelCrypto.encodePublicKey(host.getPublic());

        var atClient = ChannelCrypto.deriveKey(client.getPrivate(), ChannelCrypto.decodePublicKey(hostKey), "abc.def");
        var atHost = ChannelCrypto.deriveKey(host.getPrivate(), ChannelCrypto.decodePublicKey(clientKey), "abc.def");

        assertArrayEquals(atClient, atHost);
        assertEquals(32, atHost.length);
    }

    @Test
    public void anotherSessionId_givesAnotherKey() {
        var client = ChannelCrypto.newKeyPair();
        var host = ChannelCrypto.newKeyPair();

        assertNotEquals(
                HEX.formatHex(ChannelCrypto.deriveKey(client.getPrivate(), host.getPublic(), "one")),
                HEX.formatHex(ChannelCrypto.deriveKey(client.getPrivate(), host.getPublic(), "two")));
    }

    @Test
    public void theSameTextTwice_ciphersDifferently_andBothDecipher() {
        var key = new byte[32];

        var first = ChannelCrypto.cipher(key, "s3cret");
        var second = ChannelCrypto.cipher(key, "s3cret");

        assertNotEquals(first, second);
        assertEquals("s3cret", ChannelCrypto.decipher(key, first));
        assertEquals("s3cret", ChannelCrypto.decipher(key, second));
        assertEquals("12-byte IV + 6 bytes + 16-byte tag", 34, Base64.getDecoder().decode(first).length);
    }

    @Test
    public void aTamperedByte_isRefused() {
        var key = new byte[32];
        var bytes = Base64.getDecoder().decode(ChannelCrypto.cipher(key, "s3cret"));
        bytes[20] ^= 1;

        var tampered = Base64.getEncoder().encodeToString(bytes);
        assertThrows(IllegalArgumentException.class, () -> ChannelCrypto.decipher(key, tampered));
    }

    @Test
    public void anotherKey_isRefused() {
        var value = ChannelCrypto.cipher(new byte[32], "s3cret");
        var other = new byte[32];
        other[0] = 1;

        assertThrows(IllegalArgumentException.class, () -> ChannelCrypto.decipher(other, value));
    }

    @Test
    public void notAPublicKey_isRefused() {
        assertThrows(IllegalArgumentException.class, () -> ChannelCrypto.decodePublicKey("bm90IGEga2V5"));
    }
}
