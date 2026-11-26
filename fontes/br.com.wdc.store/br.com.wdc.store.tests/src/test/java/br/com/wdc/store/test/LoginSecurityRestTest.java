package br.com.wdc.store.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.BusinessException;
import br.com.wdc.framework.domain.exception.InvalidRequestException;
import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.framework.domain.projection.ProjectionValues;
import br.com.wdc.framework.domain.security.AuthenticationService;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.domain.security.Scram;
import br.com.wdc.framework.domain.security.ScramVerifier;
import br.com.wdc.store.domain.user.User;
import br.com.wdc.store.domain.user.UserCodec;
import br.com.wdc.store.domain.user.UserCriteria;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.client.AuthJson;
import br.com.wdc.store.persistence.client.HttpUserRepository;
import br.com.wdc.store.persistence.client.OkHttpTransport;
import br.com.wdc.store.persistence.client.RestAuthClient;
import br.com.wdc.store.scripts.sgbd.DBReset;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.RestSession;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * The SCRAM-SHA-256 sign-in over {@code /api/auth}, with security on: the exchange, what a refusal tells (nothing),
 * the single-use nonce bound to its user, the wait after too many failures, and the session each access token is
 * bound to.
 */
public class LoginSecurityRestTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.REST).withSecurity();

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    private final Scram scram = new Scram(new JceCryptoProvider());

    @Test
    public void theSeedStoresVerifiers_andTheClientSignsIn_checkingTheServer() {
        var stored = UserRepository.BEAN.get().fetchById(DBReset.JOHN_DOE_ID, new User()
                .withPassword(ProjectionValues.INSTANCE.str)).password();
        var verifier = ScramVerifier.parse(stored);
        assertNotNull(stored, verifier);
        assertEquals(ScramVerifier.DEFAULT_ITERATIONS, verifier.iterations());

        // RestAuthClient.login refuses a server whose signature does not match
        var session = RestSession.signIn(env, "jdoe");
        assertNotNull(session.auth().accessToken());
    }

    @Test
    public void theChallengeCarriesTheUsersSaltAndIterations() {
        var stored = ScramVerifier.parse(UserRepository.BEAN.get().fetchById(DBReset.JOHN_DOE_ID, new User()
                .withPassword(ProjectionValues.INSTANCE.str)).password());

        var challenge = challenge("jdoe");
        assertArrayEquals(stored.salt(), Base64.getDecoder().decode(challenge.salt()));
        assertEquals(stored.iterations(), challenge.iterations());
        assertNotEquals("a new nonce each time", challenge.nonce(), challenge("jdoe").nonce());
    }

    @Test
    public void anUnknownUser_getsStableMadeUpParameters() {
        var first = challenge("nobody");
        var second = challenge("nobody");
        assertEquals("the same salt for the same name", first.salt(), second.salt());
        assertEquals(ScramVerifier.DEFAULT_ITERATIONS, first.iterations());
        assertEquals(16, Base64.getDecoder().decode(first.salt()).length);
        assertNotEquals("another name, another salt", first.salt(), challenge("somebody").salt());

        // and the refusal is the same as a wrong password's
        assertEquals(401, login("nobody", "whatever").statusCode());
        assertEquals(401, login("jdoe", "wrong").statusCode());
    }

    @Test
    public void aNonceIsGoodOnce_andForItsUserOnly() throws Exception {
        var challenge = challenge("jdoe");
        var proof = scram.respond("jdoe", "jdoe", challenge).proof();
        assertEquals(200, post("/api/auth/login", AuthJson.loginRequest("jdoe", proof, challenge.nonce())).statusCode());
        assertEquals("used once", 401,
                post("/api/auth/login", AuthJson.loginRequest("jdoe", proof, challenge.nonce())).statusCode());

        var forJroe = challenge("jroe");
        var jdoesProof = scram.respond("jdoe", "jdoe", forJroe).proof();
        assertEquals("someone else's nonce", 401,
                post("/api/auth/login", AuthJson.loginRequest("jdoe", jdoesProof, forJroe.nonce())).statusCode());
    }

    @Test
    public void aWrongPassword_isRefused_andTheServerSeesNoPassword() {
        var auth = new RestAuthClient(new OkHttpTransport(env.baseUrl()));
        var e = assertThrows(BusinessException.class, () -> auth.login("jdoe", "not-jdoe"));
        assertTrue(e.getMessage(), e.getMessage().contains("401"));
        assertFalse(auth.isAuthenticated());
    }

    @Test
    public void afterFiveFailures_theUserMustWait_evenWithTheRightPassword() throws Exception {
        for (int i = 0; i < 5; i++) {
            assertEquals("failure " + (i + 1) + " is free", 401, login("jroe", "wrong").statusCode());
        }
        // the sixth failure starts the wait
        assertEquals(401, login("jroe", "wrong").statusCode());

        var refused = login("jroe", "jroe");
        assertEquals(429, refused.statusCode());
        var retryAfter = Long.parseLong(refused.headers().firstValue("Retry-After").orElseThrow());
        assertTrue("about 30 s: " + retryAfter, retryAfter > 0 && retryAfter <= 30);
        assertTrue(refused.body(), refused.body().contains("\"retryAfter\""));

        // a REST client gets it as the exception, with the wait
        var auth = new RestAuthClient(new OkHttpTransport(env.baseUrl()));
        var e = assertThrows(TooManyRequestsException.class, () -> auth.login("jroe", "jroe"));
        assertNotNull(e.retryAfterSeconds());

        // another account is not affected
        assertNotNull(RestSession.signIn(env, "jdoe").auth().accessToken());
    }

    @Test
    public void eachSessionOfAUser_hasItsOwnKey() {
        var first = RestSession.signIn(env, "admin");
        var second = RestSession.signIn(env, "admin");
        assertNotEquals(first.auth().publicKeyBase64(), second.auth().publicKeyBase64());

        // a password encrypted with one session's key is read by that session only
        var encrypted = first.auth().encryptPassword("secret");
        assertThrows(InvalidRequestException.class, () -> second.users().insert(newUser("other", encrypted)));
        assertTrue(first.users().insert(newUser("mine", encrypted)));
        assertNotNull(RestSession.signIn(env, "mine", "secret").auth().accessToken());
    }

    @Test
    public void thePersistentToken_doesNotOpenTheApi() {
        var persistent = AuthenticationService.BEAN.get().createPersistentToken(DBReset.ADMIN_ID, "admin");
        var transport = new OkHttpTransport(env.baseUrl());
        transport.setAccessTokenSupplier(() -> persistent);
        var users = new HttpUserRepository(transport, new UserCodec());
        var e = assertThrows(BusinessException.class, () -> users.fetch(new UserCriteria(), 0, 1));
        assertTrue(e.getMessage(), e.getMessage().contains("401"));

        // and an access token is not a persistent one
        var access = RestSession.signIn(env, "admin").auth().accessToken();
        assertNull(AuthenticationService.BEAN.get().loginWithPersistentToken(access));
        assertNotNull(AuthenticationService.BEAN.get().loginWithPersistentToken(persistent));
    }

    // :: Helpers

    private static User newUser(String userName, String password) {
        return new User()
                .withUserName(userName)
                .withPassword(password)
                .withName("A New User")
                .withRoles("CUSTOMER");
    }

    private static AuthenticationService.ChallengeResult challenge(String userName) {
        var transport = new OkHttpTransport(env.baseUrl());
        return AuthJson.readChallenge(transport.postJsonPublic("/api/auth/challenge", AuthJson.challengeRequest(userName)));
    }

    /** A whole sign-in by hand, the answer of the login as it came. */
    private HttpResponse<String> login(String userName, String password) {
        try {
            var challenge = challenge(userName);
            var proof = scram.respond(userName, password, challenge).proof();
            return post("/api/auth/login", AuthJson.loginRequest(userName, proof, challenge.nonce()));
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static HttpResponse<String> post(String path, String body) throws Exception {
        try (var http = HttpClient.newHttpClient()) {
            return http.send(HttpRequest.newBuilder(URI.create(env.baseUrl() + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
