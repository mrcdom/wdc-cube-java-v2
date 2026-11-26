package br.com.wdc.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.time.Instant;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;

import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.store.persistence.impl.security.LoginThrottle;
import br.com.wdc.store.test.util.MutableClock;
import br.com.wdc.store.test.util.ResetDatabaseRule;
import br.com.wdc.store.test.util.TestEnvironment;

/** The per-account wait after failed sign-ins, kept in {@code EN_LOGIN_FAILURE}. */
public class LoginThrottleTest {

    @ClassRule
    public static TestEnvironment env = new TestEnvironment(TestEnvironment.Mode.LOCAL);

    @Rule
    public ExternalResource resetDb = new ResetDatabaseRule(env);

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T12:00:00Z"));
    private final LoginThrottle throttle = new LoginThrottle(5, Duration.ofSeconds(30), Duration.ofMinutes(15),
            Duration.ofHours(1), clock);

    @Test
    public void fiveFailuresAreFree_thenTheWaitDoubles_upToTheCap() {
        fail("jdoe", 5);
        throttle.check("jdoe");

        fail("jdoe", 1);
        assertEquals(30, waitOf("jdoe"));
        clock.advance(Duration.ofSeconds(30));
        throttle.check("jdoe");

        fail("jdoe", 1);
        assertEquals(60, waitOf("jdoe"));
        fail("jdoe", 1);
        assertEquals(120, waitOf("jdoe"));

        fail("jdoe", 10);
        assertEquals("never more than the cap", 15 * 60, waitOf("jdoe"));
    }

    @Test
    public void aSuccessClearsTheCount() {
        fail("jdoe", 6);
        throttle.succeeded("jdoe");
        throttle.check("jdoe");
        fail("jdoe", 5);
        throttle.check("jdoe");
    }

    @Test
    public void oldFailuresAreForgotten() {
        fail("jdoe", 6);
        clock.advance(Duration.ofHours(1).plusSeconds(1));
        throttle.check("jdoe");

        // the count starts over
        fail("jdoe", 5);
        throttle.check("jdoe");
        fail("jdoe", 1);
        assertEquals(30, waitOf("jdoe"));

        clock.advance(Duration.ofHours(2));
        throttle.evictExpired();
        fail("jdoe", 5);
        throttle.check("jdoe");
    }

    @Test
    public void theAccountIsTheLogin_whateverTheCase_andOthersAreNotAffected() {
        fail("JDoe ", 6);
        assertEquals(30, waitOf("jdoe"));
        throttle.check("jroe");

        // a name longer than the column is kept by its hash
        var longName = "x".repeat(300);
        fail(longName, 6);
        assertEquals(30, waitOf(longName));
    }

    private void fail(String userName, int times) {
        for (int i = 0; i < times; i++) {
            throttle.failed(userName);
        }
    }

    private long waitOf(String userName) {
        return assertThrows(TooManyRequestsException.class, () -> throttle.check(userName)).retryAfterSeconds();
    }
}
