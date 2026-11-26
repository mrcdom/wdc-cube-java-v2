package br.com.wdc.store.persistence.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.Duration;
import java.time.Instant;

import org.junit.Test;

import br.com.wdc.framework.domain.exception.TooManyRequestsException;
import br.com.wdc.store.test.util.MutableClock;

/** The per-address limit of {@code /api/auth}: a fixed window per clock minute. */
public class AuthRateLimitTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-11-26T12:00:15Z"));

    @Test
    public void aboveTheLimit_waitsForTheMinuteToTurn() {
        var limit = new AuthRateLimit(3, clock);
        limit.check("10.0.0.1");
        limit.check("10.0.0.1");
        limit.check("10.0.0.1");
        var e = assertThrows(TooManyRequestsException.class, () -> limit.check("10.0.0.1"));
        assertEquals(Long.valueOf(45), e.retryAfterSeconds());

        // another address has its own count
        limit.check("10.0.0.2");

        clock.advance(Duration.ofSeconds(45));
        limit.check("10.0.0.1");
        assertEquals("the old windows went at the turn of the minute", 1, limit.size());
    }

    @Test
    public void zeroIsNoLimit() {
        var limit = new AuthRateLimit(0, clock);
        for (int i = 0; i < 1000; i++) {
            limit.check("10.0.0.1");
        }
    }
}
