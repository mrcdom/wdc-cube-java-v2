package br.com.wdc.framework.domain.security;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.util.Base64;

import org.junit.Test;

/** SCRAM-SHA-256: the RFC 7677 test vector, the proof, the server's signature and the verifier the server keeps. */
public class ScramTest {

    private final Scram scram = new Scram(new JceCryptoProvider());

    @Test
    public void rfc7677TestVector() {
        // RFC 7677, section 3: user "user", password "pencil"
        var salt = Base64.getDecoder().decode("W22ZaJ0SNY7soEsUEjb6gQ==");
        var authMessage = "n=user,r=rOprNGfwEbeRWgbNEkqO,"
                + "r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096,"
                + "c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0";
        var salted = scram.saltedPassword("pencil", salt, 4096);

        assertEquals("dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=",
                Base64.getEncoder().encodeToString(scram.clientProof(salted, authMessage)));
        assertEquals("6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=",
                Base64.getEncoder().encodeToString(scram.serverSignature(scram.serverKey(salted), authMessage)));
    }

    @Test
    public void theVerifierAcceptsTheRightPassword_only() {
        var salt = salt(1);
        var verifier = scram.verifier("digest-of-secret", salt, 64);
        var authMessage = Scram.authMessage("john", "nonce-1");

        var right = scram.clientProof(scram.saltedPassword("digest-of-secret", salt, 64), authMessage);
        var wrong = scram.clientProof(scram.saltedPassword("digest-of-other", salt, 64), authMessage);
        assertTrue(scram.verifyProof(verifier, authMessage, right));
        assertFalse(scram.verifyProof(verifier, authMessage, wrong));
        assertFalse("a proof is good for one exchange", scram.verifyProof(verifier, Scram.authMessage("john", "nonce-2"), right));
        assertFalse("and for one user", scram.verifyProof(verifier, Scram.authMessage("jane", "nonce-1"), right));
        assertFalse(scram.verifyProof(verifier, authMessage, new byte[0]));
    }

    @Test
    public void theClientRecognizesTheServer_byItsSignature() {
        var salt = salt(7);
        var digest = PasswordUtil.hashPassword("secret", new JceCryptoProvider());
        var verifier = scram.verifier(digest, salt, 64);
        var challenge = new AuthenticationService.ChallengeResult("nonce-1", Base64.getEncoder().encodeToString(salt), 64,
                Instant.MAX);

        var response = scram.respond("john", "secret", challenge);
        var authMessage = Scram.authMessage("john", "nonce-1");
        assertTrue(scram.verifyProof(verifier, authMessage, Base64.getDecoder().decode(response.proof())));
        assertTrue(response.serverIsAuthentic(
                Base64.getEncoder().encodeToString(scram.serverSignature(verifier.serverKey(), authMessage))));
        assertFalse(response.serverIsAuthentic(Base64.getEncoder().encodeToString(new byte[32])));
        assertFalse(response.serverIsAuthentic(null));
    }

    @Test
    public void aVerifierIsStoredInPostgresqlsEncoding_andReadBack() {
        var verifier = scram.verifier("digest", salt(0), 4096);
        var text = verifier.encode();
        assertTrue(text, text.startsWith("SCRAM-SHA-256$4096:"));

        var read = ScramVerifier.parse(text);
        assertNotNull(read);
        assertEquals(4096, read.iterations());
        assertArrayEquals(verifier.salt(), read.salt());
        assertArrayEquals(verifier.storedKey(), read.storedKey());
        assertArrayEquals(verifier.serverKey(), read.serverKey());
        assertNull("an old digest is not a verifier", ScramVerifier.parse("5f4dcc3b5aa765d61d8327deb882cf99"));
        assertNull(ScramVerifier.parse("SCRAM-SHA-256$0:AA==$AA==:AA=="));
        assertNull(ScramVerifier.parse("SCRAM-SHA-256$x:AA==$AA==:AA=="));
        assertNull(ScramVerifier.parse(null));
    }

    @Test
    public void aStoredVerifierMatchesThePassword_itCameFrom() {
        var verifier = ScramVerifier.parse(PasswordVerifiers.fromPassword("secret"));
        assertNotNull(verifier);
        assertEquals(ScramVerifier.DEFAULT_ITERATIONS, verifier.iterations());
        assertEquals(ScramVerifier.SALT_SIZE, verifier.salt().length);
        assertTrue(scram.matches(verifier, "secret"));
        assertFalse(scram.matches(verifier, "Secret"));

        // a digest converts to the verifier its password would get
        var fromDigest = ScramVerifier.parse(PasswordVerifiers.fromDigest(PasswordUtil.hashPassword("secret", new JceCryptoProvider()) + "  "));
        assertTrue(scram.matches(fromDigest, "secret"));
    }

    private static byte[] salt(int step) {
        var salt = new byte[ScramVerifier.SALT_SIZE];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) (i * step);
        }
        return salt;
    }
}
