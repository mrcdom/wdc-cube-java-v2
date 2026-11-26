package br.com.wdc.store.view.remote.shell.cn1.bridge;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.KeyAgreement;

import org.junit.jupiter.api.Test;

import com.codename1.util.BigInteger;

/**
 * {@link P256} against the JVM's own P-256 (JCA): the same shared secret both ways, each side's public keys read by
 * the other, and invalid keys refused. Runs on the plain JVM: {@link BigInteger} is pure Java.
 */
class P256Test {

    private static final int ROUNDS = 16;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Test
    void generatorIsOnTheCurveAndHasOrderN() {
        assertTrue(P256.isOnCurve(P256.GX, P256.GY));
        BigInteger[] g = { P256.GX, P256.GY, BigInteger.ONE };
        // n·G = infinity; (n-1)·G = -G
        BigInteger[] nMinusOne = P256.toAffine(P256.multiply(P256.N.subtract(BigInteger.ONE), g));
        assertEquals(P256.GX, nMinusOne[0]);
        assertEquals(P256.P.subtract(P256.GY), nMinusOne[1]);
        assertEquals(0, P256.add(P256.multiply(P256.N.subtract(BigInteger.ONE), g), g)[2].signum());
    }

    @Test
    void publicKeyOfOneIsTheGenerator() throws Exception {
        ECPublicKey jca = jcaPublic(P256.publicKey(BigInteger.ONE));
        assertEquals(new java.math.BigInteger(P256.GX.toString(16), 16), jca.getW().getAffineX());
        assertEquals(new java.math.BigInteger(P256.GY.toString(16), 16), jca.getW().getAffineY());
    }

    @Test
    void agreesWithJcaBothWays() throws Exception {
        for (int i = 0; i < ROUNDS; i++) {
            KeyPair jca = jcaKeyPair();
            BigInteger own = randomPrivateKey();
            byte[] ownSpki = P256.publicKey(own);

            byte[] ours = P256.sharedSecret(own, jca.getPublic().getEncoded());

            KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
            agreement.init(jca.getPrivate());
            agreement.doPhase(jcaPublic(ownSpki), true);
            byte[] theirs = agreement.generateSecret();

            assertEquals(32, ours.length);
            assertArrayEquals(theirs, ours);
        }
    }

    @Test
    void ourSpkiDecodesInJcaAndJcaSpkiDecodesInOurs() throws Exception {
        for (int i = 0; i < ROUNDS; i++) {
            BigInteger own = randomPrivateKey();
            byte[] spki = P256.publicKey(own);
            assertEquals(P256.SPKI_LENGTH, spki.length);
            ECPublicKey decoded = jcaPublic(spki);
            assertArrayEquals(spki, decoded.getEncoded());

            KeyPair jca = jcaKeyPair();
            ECPublicKey jcaPublic = (ECPublicKey) jca.getPublic();
            BigInteger[] point = P256.decodePublicKey(jcaPublic.getEncoded());
            assertEquals(jcaPublic.getW().getAffineX(), new java.math.BigInteger(point[0].toString(16), 16));
            assertEquals(jcaPublic.getW().getAffineY(), new java.math.BigInteger(point[1].toString(16), 16));
        }
    }

    @Test
    void coordinatesWithLeadingZerosKeepTheirLength() throws Exception {
        // a key whose x or y starts with a zero byte must still be written on 32 bytes
        int found = 0;
        for (int i = 0; i < 2000 && found < 2; i++) {
            BigInteger own = randomPrivateKey();
            byte[] spki = P256.publicKey(own);
            if (spki[27] == 0 || spki[59] == 0) {
                found++;
                assertArrayEquals(spki, jcaPublic(spki).getEncoded());
            }
        }
        assertTrue(found > 0, "no key with a leading zero byte in 2000 tries");
    }

    @Test
    void refusesWhatIsNotAP256PublicKey() throws Exception {
        byte[] valid = jcaKeyPair().getPublic().getEncoded();

        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(null));
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(java.util.Arrays.copyOf(valid, 90)));

        byte[] header = valid.clone();
        header[20] ^= 1; // the curve's OID
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(header));

        byte[] compressed = valid.clone();
        compressed[26] = 0x02;
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(compressed));

        byte[] offCurve = valid.clone();
        offCurve[90] ^= 1; // y changed
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(offCurve));

        byte[] zero = valid.clone();
        java.util.Arrays.fill(zero, 27, 91, (byte) 0); // (0, 0): the usual encoding of infinity
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(zero));

        // x = p: out of the field
        byte[] xIsP = valid.clone();
        byte[] p = unsigned32(P256.P);
        System.arraycopy(p, 0, xIsP, 27, 32);
        assertThrows(IllegalArgumentException.class, () -> P256.decodePublicKey(xIsP));

        BigInteger own = randomPrivateKey();
        assertThrows(IllegalArgumentException.class, () -> P256.sharedSecret(own, offCurve));
    }

    @Test
    void refusesPrivateKeysOutOfRange() {
        assertFalse(P256.isPrivateKey(BigInteger.ZERO));
        assertFalse(P256.isPrivateKey(P256.N));
        assertTrue(P256.isPrivateKey(P256.N.subtract(BigInteger.ONE)));
        assertThrows(IllegalArgumentException.class, () -> P256.publicKey(BigInteger.ZERO));
        assertThrows(IllegalArgumentException.class, () -> P256.publicKey(P256.N));
    }

    // -- helpers --

    private static BigInteger randomPrivateKey() {
        while (true) {
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            BigInteger d = new BigInteger(1, bytes);
            if (P256.isPrivateKey(d)) {
                return d;
            }
        }
    }

    private static KeyPair jcaKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"), RANDOM);
        return generator.generateKeyPair();
    }

    private static ECPublicKey jcaPublic(byte[] spki) throws Exception {
        PublicKey key = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(spki));
        return (ECPublicKey) key;
    }

    private static byte[] unsigned32(BigInteger v) {
        byte[] raw = v.toByteArray();
        byte[] out = new byte[32];
        System.arraycopy(raw, raw.length - 32, out, 0, 32);
        return out;
    }
}
