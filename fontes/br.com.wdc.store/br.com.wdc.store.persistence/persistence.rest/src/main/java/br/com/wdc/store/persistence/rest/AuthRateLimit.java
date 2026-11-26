package br.com.wdc.store.persistence.rest;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import br.com.wdc.framework.domain.exception.TooManyRequestsException;

/**
 * Limits the {@code /api/auth} requests (challenge, login, refresh) per client address and minute: they are open — no
 * token yet —, and a password guessed by brute force goes through here. Fixed window per clock minute; above the
 * limit, {@link TooManyRequestsException} (429 with {@code Retry-After}, the seconds until the minute turns).
 * <p>
 * The count is per instance, in memory: with N instances, an address gets up to N times the limit. The address is the
 * connection's: behind a proxy, every client is the proxy's. One account attacked from many addresses is the
 * {@code LoginThrottle}'s, kept in the database.
 */
final class AuthRateLimit {

    private static final long MINUTE = 60_000L;

    private static final class Window {
        final long minute;
        final AtomicLong count = new AtomicLong();

        Window(long minute) {
            this.minute = minute;
        }
    }

    private final int perMinute;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /** The minute of the last cleanup; older windows go on the first count of a new minute. */
    private final AtomicLong cleaned = new AtomicLong(Long.MIN_VALUE);

    /** {@code perMinute <= 0}: no limit. */
    AuthRateLimit(int perMinute) {
        this(perMinute, Clock.systemUTC());
    }

    AuthRateLimit(int perMinute, Clock clock) {
        this.perMinute = perMinute;
        this.clock = clock;
    }

    /** Counts a request from {@code address}; above the limit, {@link TooManyRequestsException}. */
    void check(String address) {
        if (perMinute <= 0) {
            return;
        }
        long now = clock.millis();
        long minute = Math.floorDiv(now, MINUTE);
        long last = cleaned.get();
        if (last != minute && cleaned.compareAndSet(last, minute)) {
            windows.values().removeIf(w -> w.minute < minute);
        }
        var window = windows.compute(address == null ? "" : address,
                (key, current) -> current == null || current.minute != minute ? new Window(minute) : current);
        if (window.count.incrementAndGet() > perMinute) {
            long remaining = MINUTE - Math.floorMod(now, MINUTE);
            throw new TooManyRequestsException("Too many authentication requests", (remaining + 999) / 1000);
        }
    }

    int size() {
        return windows.size();
    }
}
