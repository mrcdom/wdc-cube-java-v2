package br.com.wdc.store.view.teavm.infra;

import static org.junit.Assert.assertArrayEquals;

import java.security.MessageDigest;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/** The browser's SHA-256 and HMAC-SHA256 give what the JCE gives, around every padding boundary and key length. */
public class Sha256Test {

    @Test
    public void digestMatchesTheJce() throws Exception {
        var random = new Random(42);
        var jce = MessageDigest.getInstance("SHA-256");
        for (int length = 0; length <= 200; length++) {
            var input = new byte[length];
            random.nextBytes(input);
            assertArrayEquals("length " + length, jce.digest(input), Sha256.digest(input));
        }
    }

    @Test
    public void hmacMatchesTheJce() throws Exception {
        var random = new Random(7);
        for (int keyLength : new int[] { 1, 20, 32, 63, 64, 65, 100 }) {
            var key = new byte[keyLength];
            random.nextBytes(key);
            var data = new byte[37];
            random.nextBytes(data);
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            assertArrayEquals("key " + keyLength, mac.doFinal(data), Sha256.hmac(key, data));
        }
    }
}
