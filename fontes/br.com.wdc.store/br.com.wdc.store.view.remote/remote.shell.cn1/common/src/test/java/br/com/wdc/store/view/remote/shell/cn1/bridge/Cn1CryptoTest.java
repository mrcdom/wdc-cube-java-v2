package br.com.wdc.store.view.remote.shell.cn1.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.codename1.impl.javase.JavaSEPort;
import com.codename1.io.Util;
import com.codename1.ui.Display;

/**
 * {@link Cn1Crypto} against the JVM's own cryptography (JCA), doing what the host does
 * ({@code ChannelCrypto}): HKDF-SHA256 on the RFC 5869 vector, a whole handshake that ends with the same key on
 * both sides, and ciphered values read by the other side.
 *
 * <p>
 * Codename One's {@code Cipher} and {@code SecureRandom} (and {@code Base64}, for its SIMD helper) run on the
 * platform's implementation; the test installs the JavaSE port (the one the simulator and the desktop build use),
 * headless, without starting the simulator.
 * </p>
 */
class Cn1CryptoTest {

    private static final String APP_ID = "aBcD1234.eFgH5678";
    private static final SecureRandom RANDOM = new SecureRandom();

    @BeforeAll
    static void installCodenameOnePort() throws Exception {
        System.setProperty("java.awt.headless", "true");
        JavaSEPort port = new JavaSEPort();
        Util.setImplementation(port); // Cipher, SecureRandom
        Field displayImpl = Display.class.getDeclaredField("impl"); // Base64 asks the display for its Simd
        displayImpl.setAccessible(true);
        displayImpl.set(null, port);
    }

    @Test
    void hkdfMatchesRfc5869TestCase1() {
        byte[] ikm = new byte[22];
        java.util.Arrays.fill(ikm, (byte) 0x0b);
        byte[] okm = Cn1Crypto.hkdfSha256(ikm, hex("000102030405060708090a0b0c"), hex("f0f1f2f3f4f5f6f7f8f9"), 42);
        assertArrayEquals(hex("3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865"),
                okm);
    }

    @Test
    void hkdfMatchesTheJcaVersion() throws Exception {
        for (int i = 0; i < 8; i++) {
            byte[] ikm = random(32);
            byte[] salt = random(1 + i * 7);
            byte[] info = Cn1Crypto.KEY_INFO.getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(jcaHkdf(ikm, salt, info, 32), Cn1Crypto.hkdfSha256(ikm, salt, info, 32));
        }
    }

    @Test
    void aHandshakeAgreesOnTheHostsKey() throws Exception {
        for (int i = 0; i < 8; i++) {
            Cn1Crypto.Handshake handshake = Cn1Crypto.newHandshake();

            // the host's side, as ChannelCrypto does it
            KeyPair host = jcaKeyPair();
            byte[] hostKey = jcaHostKey(host, handshake.clientKey());

            Cn1Crypto client = handshake.agree(Base64.getEncoder().encodeToString(host.getPublic().getEncoded()),
                    APP_ID);

            String text = "a value with ç and 🙂 #" + i;
            assertEquals(text, client.decipher(jcaCipher(hostKey, text)));
            assertEquals(text, jcaDecipher(hostKey, client.encipher(text)));
        }
    }

    @Test
    void aHandshakeRefusesAnInvalidServerKey() {
        Cn1Crypto.Handshake handshake = Cn1Crypto.newHandshake();
        assertThrows(IllegalArgumentException.class, () -> handshake.agree(null, APP_ID));
        assertThrows(IllegalArgumentException.class, () -> handshake.agree("not base64 !", APP_ID));
        assertThrows(IllegalArgumentException.class, () -> handshake.agree(handshake.clientKey().substring(4), APP_ID));
    }

    @Test
    void cipheredValuesCrossBetweenCodenameOneAndJca() throws Exception {
        byte[] key = random(32);
        Cn1Crypto crypto = new Cn1Crypto(key);

        String ours = crypto.encipher("secret");
        byte[] raw = Base64.getDecoder().decode(ours);
        assertEquals(12 + 6 + 16, raw.length);
        assertEquals("secret", jcaDecipher(key, ours));
        assertEquals("secret", crypto.decipher(jcaCipher(key, "secret")));
        assertEquals("", crypto.decipher(crypto.encipher("")));
    }

    @Test
    void everyValueHasItsOwnIv() {
        Cn1Crypto crypto = new Cn1Crypto(random(32));
        String a = crypto.encipher("same");
        String b = crypto.encipher("same");
        assertNotEquals(a, b);
        assertNotEquals(a.substring(0, 16), b.substring(0, 16));
    }

    @Test
    void refusesWhatDoesNotAuthenticate() throws Exception {
        byte[] key = random(32);
        Cn1Crypto crypto = new Cn1Crypto(key);

        byte[] tampered = Base64.getDecoder().decode(jcaCipher(key, "secret"));
        tampered[tampered.length - 1] ^= 1;
        assertNull(crypto.decipher(Base64.getEncoder().encodeToString(tampered)));
        assertNull(crypto.decipher(new Cn1Crypto(random(32)).encipher("secret"))); // another connection's key
        assertNull(crypto.decipher("%%%"));
        assertNull(crypto.decipher(Base64.getEncoder().encodeToString(new byte[27])));
        assertNull(crypto.decipher(null));
    }

    // -- the host's side, with JCA (same recipe as ChannelCrypto) --

    private static KeyPair jcaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    private static byte[] jcaHostKey(KeyPair host, String clientKey) throws Exception {
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(host.getPrivate());
        agreement.doPhase(KeyFactory.getInstance("EC")
                .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(clientKey))), true);
        return jcaHkdf(agreement.generateSecret(), APP_ID.getBytes(StandardCharsets.UTF_8),
                Cn1Crypto.KEY_INFO.getBytes(StandardCharsets.UTF_8), 32);
    }

    private static byte[] jcaHkdf(byte[] ikm, byte[] salt, byte[] info, int length) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(salt, "HmacSHA256"));
        byte[] prk = mac.doFinal(ikm);
        mac.init(new SecretKeySpec(prk, "HmacSHA256"));
        byte[] out = new byte[length];
        byte[] block = new byte[0];
        int written = 0;
        for (int counter = 1; written < length; counter++) {
            mac.update(block);
            mac.update(info);
            mac.update((byte) counter);
            block = mac.doFinal();
            int n = Math.min(block.length, length - written);
            System.arraycopy(block, 0, out, written, n);
            written += n;
        }
        return out;
    }

    private static String jcaCipher(byte[] key, String text) throws Exception {
        byte[] iv = random(12);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] sealed = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[12 + sealed.length];
        System.arraycopy(iv, 0, out, 0, 12);
        System.arraycopy(sealed, 0, out, 12, sealed.length);
        return Base64.getEncoder().encodeToString(out);
    }

    private static String jcaDecipher(byte[] key, String value) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(value);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, bytes, 0, 12));
        return new String(cipher.doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
    }

    private static byte[] random(int length) {
        byte[] out = new byte[length];
        RANDOM.nextBytes(out);
        return out;
    }

    private static byte[] hex(String s) {
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
