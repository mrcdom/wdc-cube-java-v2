package br.com.wdc.store.view.remote.shell.cn1.bridge;

import com.codename1.security.SecureRandom;
import com.codename1.util.BigInteger;

/**
 * ECDH on the NIST P-256 curve (secp256r1), on Codename One's {@link BigInteger} — the platform has no key
 * agreement of its own.
 *
 * <ul>
 * <li>{@link #newPrivateKey()} — a random scalar in {@code [1, n-1]};</li>
 * <li>{@link #publicKey(BigInteger)} — {@code d·G} as an X.509 SubjectPublicKeyInfo (DER, uncompressed
 * point);</li>
 * <li>{@link #sharedSecret(BigInteger, byte[])} — the 32-byte x-coordinate of {@code d·Q}, after checking that
 * the peer's key is a point of the curve.</li>
 * </ul>
 *
 * <p>
 * Points are kept in Jacobian coordinates ({@code x = X/Z², y = Y/Z³}, infinity when {@code Z = 0}); the scalar
 * multiplication is a Montgomery ladder of a fixed 256 steps, each step one addition and one doubling whatever
 * the bit. {@link BigInteger} arithmetic is not constant-time, so the ladder only keeps the sequence of
 * operations independent of the scalar. The curve has cofactor 1: every point of the curve other than infinity
 * generates the whole group.
 * </p>
 */
public final class P256 {

    /** The field prime. */
    static final BigInteger P = hex("ffffffff00000001000000000000000000000000ffffffffffffffffffffffff");
    /** The curve's {@code b} ({@code a = -3}). */
    static final BigInteger B = hex("5ac635d8aa3a93e7b3ebbd55769886bc651d06b0cc53b0f63bce3c3e27d2604b");
    /** The order of the group. */
    static final BigInteger N = hex("ffffffff00000000ffffffffffffffffbce6faada7179e84f3b9cac2fc632551");
    static final BigInteger GX = hex("6b17d1f2e12c4247f8bce6e563a440f277037d812deb33a0f4a13945d898c296");
    static final BigInteger GY = hex("4fe342e2fe1a7f9b8ee7eb4a7c0f9e162bce33576b315ececbb6406837bf51f5");

    /** The SubjectPublicKeyInfo of a P-256 key up to the point: id-ecPublicKey, prime256v1, BIT STRING. */
    private static final byte[] SPKI_PREFIX = bytes("3059301306072a8648ce3d020106082a8648ce3d030107034200");
    /** Prefix, then 0x04 ‖ X(32) ‖ Y(32). */
    static final int SPKI_LENGTH = 91;

    private static final int COORDINATE_LENGTH = 32;
    private static final int SCALAR_BITS = 256;

    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final BigInteger P_MINUS_2 = P.subtract(TWO);
    private static final BigInteger THREE = BigInteger.valueOf(3);
    private static final BigInteger FOUR = BigInteger.valueOf(4);
    private static final BigInteger EIGHT = BigInteger.valueOf(8);

    private P256() {
        // NOOP
    }

    /** A new private key: a uniformly random scalar in {@code [1, n-1]} (rejection sampling). */
    public static BigInteger newPrivateKey() {
        while (true) {
            BigInteger d = new BigInteger(1, SecureRandom.bytes(COORDINATE_LENGTH));
            if (isPrivateKey(d)) {
                return d;
            }
        }
    }

    /** Whether {@code d} is in {@code [1, n-1]}. */
    static boolean isPrivateKey(BigInteger d) {
        return d.signum() > 0 && d.compareTo(N) < 0;
    }

    /** The public key {@code d·G}, as an X.509 SubjectPublicKeyInfo (DER). */
    public static byte[] publicKey(BigInteger d) {
        requirePrivateKey(d);
        BigInteger[] q = toAffine(multiply(d, new BigInteger[] { GX, GY, BigInteger.ONE }));
        byte[] out = new byte[SPKI_LENGTH];
        System.arraycopy(SPKI_PREFIX, 0, out, 0, SPKI_PREFIX.length);
        out[SPKI_PREFIX.length] = 0x04;
        writeCoordinate(q[0], out, SPKI_PREFIX.length + 1);
        writeCoordinate(q[1], out, SPKI_PREFIX.length + 1 + COORDINATE_LENGTH);
        return out;
    }

    /**
     * Reads a peer's public key: {@code {x, y}}.
     *
     * @throws IllegalArgumentException if it is not a SubjectPublicKeyInfo of an uncompressed P-256 point that
     *                                  lies on the curve
     */
    public static BigInteger[] decodePublicKey(byte[] spki) {
        if (spki == null || spki.length != SPKI_LENGTH) {
            throw new IllegalArgumentException("Not a P-256 SubjectPublicKeyInfo: length");
        }
        for (int i = 0; i < SPKI_PREFIX.length; i++) {
            if (spki[i] != SPKI_PREFIX[i]) {
                throw new IllegalArgumentException("Not a P-256 SubjectPublicKeyInfo: header");
            }
        }
        if (spki[SPKI_PREFIX.length] != 0x04) {
            throw new IllegalArgumentException("Not an uncompressed point");
        }
        BigInteger x = readCoordinate(spki, SPKI_PREFIX.length + 1);
        BigInteger y = readCoordinate(spki, SPKI_PREFIX.length + 1 + COORDINATE_LENGTH);
        if (!isOnCurve(x, y)) {
            throw new IllegalArgumentException("The point is not on the P-256 curve");
        }
        return new BigInteger[] { x, y };
    }

    /**
     * The ECDH shared secret: the x-coordinate of {@code d·Q}, 32 bytes big-endian.
     *
     * @throws IllegalArgumentException if the peer's key is not a valid P-256 public key
     */
    public static byte[] sharedSecret(BigInteger d, byte[] peerSpki) {
        requirePrivateKey(d);
        BigInteger[] q = decodePublicKey(peerSpki);
        BigInteger[] s = multiply(d, new BigInteger[] { q[0], q[1], BigInteger.ONE });
        if (s[2].signum() == 0) {
            throw new IllegalArgumentException("The shared point is the point at infinity");
        }
        byte[] out = new byte[COORDINATE_LENGTH];
        writeCoordinate(toAffine(s)[0], out, 0);
        return out;
    }

    /** Whether {@code (x, y)} is a point of the curve: coordinates in the field and {@code y² = x³ - 3x + b}. */
    static boolean isOnCurve(BigInteger x, BigInteger y) {
        if (x.signum() < 0 || x.compareTo(P) >= 0 || y.signum() < 0 || y.compareTo(P) >= 0) {
            return false;
        }
        BigInteger left = y.multiply(y).mod(P);
        BigInteger right = x.multiply(x).multiply(x).subtract(THREE.multiply(x)).add(B).mod(P);
        return left.equals(right);
    }

    // -- group arithmetic, Jacobian coordinates {X, Y, Z} --

    /** {@code k·p} by a Montgomery ladder over all 256 bits of {@code k}. */
    static BigInteger[] multiply(BigInteger k, BigInteger[] p) {
        BigInteger[] r0 = infinity();
        BigInteger[] r1 = p;
        for (int i = SCALAR_BITS - 1; i >= 0; i--) {
            if (k.testBit(i)) {
                r0 = add(r0, r1);
                r1 = twice(r1);
            } else {
                r1 = add(r0, r1);
                r0 = twice(r0);
            }
        }
        return r0;
    }

    private static BigInteger[] infinity() {
        return new BigInteger[] { BigInteger.ONE, BigInteger.ONE, BigInteger.ZERO };
    }

    /** Doubling with {@code a = -3} ("dbl-2001-b"). */
    static BigInteger[] twice(BigInteger[] p) {
        BigInteger x1 = p[0];
        BigInteger y1 = p[1];
        BigInteger z1 = p[2];
        if (z1.signum() == 0 || y1.signum() == 0) {
            return infinity();
        }
        BigInteger delta = z1.multiply(z1).mod(P);
        BigInteger gamma = y1.multiply(y1).mod(P);
        BigInteger beta = x1.multiply(gamma).mod(P);
        BigInteger alpha = THREE.multiply(x1.subtract(delta)).multiply(x1.add(delta)).mod(P);
        BigInteger x3 = alpha.multiply(alpha).subtract(EIGHT.multiply(beta)).mod(P);
        BigInteger yz = y1.add(z1);
        BigInteger z3 = yz.multiply(yz).subtract(gamma).subtract(delta).mod(P);
        BigInteger y3 = alpha.multiply(FOUR.multiply(beta).subtract(x3))
                .subtract(EIGHT.multiply(gamma).multiply(gamma))
                .mod(P);
        return new BigInteger[] { x3, y3, z3 };
    }

    /** General addition ("add-2007-bl" without the shortcuts), with infinity and equal points handled. */
    static BigInteger[] add(BigInteger[] p, BigInteger[] q) {
        if (p[2].signum() == 0) {
            return q;
        }
        if (q[2].signum() == 0) {
            return p;
        }
        BigInteger z1z1 = p[2].multiply(p[2]).mod(P);
        BigInteger z2z2 = q[2].multiply(q[2]).mod(P);
        BigInteger u1 = p[0].multiply(z2z2).mod(P);
        BigInteger u2 = q[0].multiply(z1z1).mod(P);
        BigInteger s1 = p[1].multiply(q[2]).multiply(z2z2).mod(P);
        BigInteger s2 = q[1].multiply(p[2]).multiply(z1z1).mod(P);
        BigInteger h = u2.subtract(u1).mod(P);
        BigInteger r = s2.subtract(s1).mod(P);
        if (h.signum() == 0) {
            return r.signum() == 0 ? twice(p) : infinity();
        }
        BigInteger hh = h.multiply(h).mod(P);
        BigInteger hhh = h.multiply(hh).mod(P);
        BigInteger v = u1.multiply(hh).mod(P);
        BigInteger x3 = r.multiply(r).subtract(hhh).subtract(TWO.multiply(v)).mod(P);
        BigInteger y3 = r.multiply(v.subtract(x3)).subtract(s1.multiply(hhh)).mod(P);
        BigInteger z3 = p[2].multiply(q[2]).multiply(h).mod(P);
        return new BigInteger[] { x3, y3, z3 };
    }

    /**
     * {@code {x, y}} of a point other than infinity. The inverse of {@code Z} is {@code Z^(p-2)} (Fermat): Codename
     * One's {@link BigInteger#modInverse} gives wrong results for this modulus.
     */
    static BigInteger[] toAffine(BigInteger[] p) {
        BigInteger zInv = p[2].modPow(P_MINUS_2, P);
        BigInteger zInv2 = zInv.multiply(zInv).mod(P);
        BigInteger x = p[0].multiply(zInv2).mod(P);
        BigInteger y = p[1].multiply(zInv2).multiply(zInv).mod(P);
        return new BigInteger[] { x, y };
    }

    // -- encoding --

    private static void requirePrivateKey(BigInteger d) {
        if (d == null || !isPrivateKey(d)) {
            throw new IllegalArgumentException("Not a P-256 private key");
        }
    }

    /** Writes a field element as 32 bytes big-endian, left-padded with zeros. */
    private static void writeCoordinate(BigInteger v, byte[] out, int offset) {
        byte[] raw = v.toByteArray(); // two's complement: may carry a leading 0x00, or be shorter than 32
        int start = 0;
        while (start < raw.length - 1 && raw[start] == 0) {
            start++;
        }
        int length = raw.length - start;
        if (length > COORDINATE_LENGTH) {
            throw new IllegalStateException("A coordinate longer than 32 bytes");
        }
        for (int i = 0; i < COORDINATE_LENGTH; i++) {
            out[offset + i] = 0;
        }
        System.arraycopy(raw, start, out, offset + COORDINATE_LENGTH - length, length);
    }

    private static BigInteger readCoordinate(byte[] in, int offset) {
        byte[] raw = new byte[COORDINATE_LENGTH];
        System.arraycopy(in, offset, raw, 0, COORDINATE_LENGTH);
        return new BigInteger(1, raw);
    }

    private static BigInteger hex(String s) {
        return new BigInteger(s, 16);
    }

    private static byte[] bytes(String hex) {
        byte[] out = new byte[hex.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(hex.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
