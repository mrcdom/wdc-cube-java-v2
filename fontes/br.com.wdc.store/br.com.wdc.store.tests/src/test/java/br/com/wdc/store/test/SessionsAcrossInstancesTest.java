package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.security.AuthenticationService.AuthResult;
import br.com.wdc.framework.domain.security.JceCryptoProvider;
import br.com.wdc.framework.domain.security.Scram;
import br.com.wdc.store.domain.user.UserRepository;
import br.com.wdc.store.persistence.impl.security.AuthenticationServiceImpl;
import br.com.wdc.store.persistence.impl.security.LoginThrottle;
import br.com.wdc.store.persistence.impl.security.SessionEvents;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/**
 * Two backend instances on one database: a session made on one is known to the other (read once from the database),
 * a challenge from one is answered on the other, and a session changed on one — renewed, signed out — stops being
 * answered from the other's memory once it is told, or once it may have missed being told.
 */
public class SessionsAcrossInstancesTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    private static final String SECRET = "sessions-across-instances-secret";

    /** The instances' notices, delivered at once to every other instance. */
    private static final class Bus {

        private final List<Member> members = new CopyOnWriteArrayList<>();

        SessionEvents join() {
            var member = new Member();
            members.add(member);
            return member;
        }

        void missedNotices() {
            members.forEach(m -> m.onMissed.run());
        }

        private final class Member implements SessionEvents {

            Consumer<String> onChanged = id -> {
            };
            Runnable onMissed = () -> {
            };

            @Override
            public void sessionChanged(String sessionId) {
                for (var other : members) {
                    if (other != this) {
                        other.onChanged.accept(sessionId);
                    }
                }
            }

            @Override
            public void listen(Consumer<String> onChanged, Runnable onMissed) {
                this.onChanged = onChanged;
                this.onMissed = onMissed;
            }
        }
    }

    private final Bus bus = new Bus();
    private final AuthenticationServiceImpl a = instance(bus.join());
    private final AuthenticationServiceImpl b = instance(bus.join());
    private final Scram scram = new Scram(new JceCryptoProvider());

    private static AuthenticationServiceImpl instance(SessionEvents events) {
        return new AuthenticationServiceImpl(UserRepository.BEAN.get(), SECRET, events, new LoginThrottle());
    }

    /** The challenge from {@code asked}, the answer to {@code answered}. */
    private AuthResult signIn(AuthenticationServiceImpl asked, AuthenticationServiceImpl answered) {
        var challenge = asked.challenge("jdoe");
        var response = scram.respond("jdoe", "jdoe", challenge);
        return answered.login("jdoe", response.proof(), challenge.nonce());
    }

    @Test
    public void aChallengeFromOne_isAnsweredOnTheOther_once() {
        var challenge = a.challenge("jdoe");
        var response = scram.respond("jdoe", "jdoe", challenge);

        assertNotNull(b.login("jdoe", response.proof(), challenge.nonce()));
        assertNull("the nonce is used up, on every instance", a.login("jdoe", response.proof(), challenge.nonce()));
    }

    @Test
    public void aSessionMadeOnOne_isKnownToTheOther() {
        var signedIn = signIn(a, a);

        var onA = a.resolveToken(signedIn.accessToken());
        var onB = b.resolveToken(signedIn.accessToken());
        assertNotNull(onB);
        assertEquals(onA.userId(), onB.userId());
        assertEquals(onA.roles(), onB.roles());
        assertEquals("the same key, read back from the database", onA.publicKeyBase64(), onB.publicKeyBase64());
        assertEquals(onA.privateKey(), onB.privateKey());
        assertSame("held once read", onB, b.resolveToken(signedIn.accessToken()));
    }

    @Test
    public void aSessionSignedOutOnOne_isRefusedByTheOther() {
        var signedIn = signIn(a, a);
        assertNotNull(b.resolveToken(signedIn.accessToken()));

        a.logout(signedIn.refreshToken());

        assertNull(a.resolveToken(signedIn.accessToken()));
        assertNull("told, b drops its copy", b.resolveToken(signedIn.accessToken()));
    }

    @Test
    public void aSessionRenewedOnOne_isTheRenewedOneOnTheOther_andTheOldRefreshTokenIsSpent() {
        var signedIn = signIn(a, a);
        var before = b.resolveToken(signedIn.accessToken());

        var renewed = a.refresh(signedIn.refreshToken());
        assertNotNull(renewed);
        assertNotEquals(signedIn.publicKey(), renewed.publicKey());

        var after = b.resolveToken(renewed.accessToken());
        assertNotEquals(before.publicKeyBase64(), after.publicKeyBase64());
        assertEquals(renewed.publicKey(), after.publicKeyBase64());
        assertNull("spent: renewed once, on whichever instance", b.refresh(signedIn.refreshToken()));
        assertNotNull("the new one renews on the other instance", b.refresh(renewed.refreshToken()));
    }

    @Test
    public void afterMissedNotices_noCopyIsTrusted() {
        var untold = new Bus();
        var c = instance(untold.join());
        var d = instance(untold.join());
        var signedIn = signIn(c, c);
        assertNotNull(d.resolveToken(signedIn.accessToken()));

        // signed out on an instance that tells no one: d still answers from its copy
        instance(SessionEvents.NONE).logout(signedIn.refreshToken());
        assertNotNull(d.resolveToken(signedIn.accessToken()));

        untold.missedNotices();
        assertNull("read again from the database, where it ended", d.resolveToken(signedIn.accessToken()));
    }
}
