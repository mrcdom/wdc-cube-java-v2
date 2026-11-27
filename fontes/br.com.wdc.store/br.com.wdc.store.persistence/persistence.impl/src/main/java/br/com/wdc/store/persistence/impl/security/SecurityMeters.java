package br.com.wdc.store.persistence.impl.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

/**
 * What the sign-in and the sessions count, in Micrometer's global registry: nothing goes anywhere until a host adds a
 * registry to it (the backend's Prometheus one, at {@code /metrics}).
 */
final class SecurityMeters {

    /** Signed in. */
    static final Counter SIGNED_IN = signIns("ok");

    /** Wrong password, unknown user, or a bad nonce. */
    static final Counter REFUSED = signIns("refused");

    /** Refused before checking, while the account must wait after too many failures. */
    static final Counter THROTTLED = signIns("throttled");

    /** Sessions renewed by their refresh token. */
    static final Counter RENEWED = Counter.builder("store.sessions.renewed")
            .description("Sessions renewed by their refresh token")
            .register(Metrics.globalRegistry);

    /** Copies of sessions dropped because another instance changed them. */
    static final Counter CHANGED_ELSEWHERE = Counter.builder("store.sessions.changed.elsewhere")
            .description("Sessions held here that another instance renewed or ended")
            .register(Metrics.globalRegistry);

    private SecurityMeters() {
    }

    private static Counter signIns(String outcome) {
        return Counter.builder("store.signins")
                .description("Sign-in attempts, by outcome")
                .tag("outcome", outcome)
                .register(Metrics.globalRegistry);
    }
}
