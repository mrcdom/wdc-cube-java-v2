package br.com.wdc.store.persistence.rest;

import java.util.Locale;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * Headers that every response of the API carries, errors and refusals included.
 *
 * <ul>
 * <li><b>Security:</b> {@code X-Content-Type-Options: nosniff} (the browser does not guess a type other than the one
 * declared), {@code X-Frame-Options: DENY} (no page frames the API), {@code Referrer-Policy: no-referrer}.</li>
 * <li><b>Cache:</b> {@code Cache-Control: no-store}, unless the route set its own: the answers carry user data.</li>
 * <li><b>Timing:</b> {@code Server-Timing: total;dur=<ms>}, the time the server spent on the request, which the
 * browser's developer tools show next to the network time.</li>
 * </ul>
 */
final class ApiResponseHeaders {

    private static final String STARTED_AT = ApiResponseHeaders.class.getName() + ".startedAt";

    private ApiResponseHeaders() {
        // NOOP
    }

    static void configure(JavalinConfig config, String prefix) {
        config.routes.before(prefix + "/api/*", ctx -> ctx.attribute(STARTED_AT, System.nanoTime()));
        config.routes.after(prefix + "/api/*", ApiResponseHeaders::write);
    }

    private static void write(Context ctx) {
        var res = ctx.res();
        res.setHeader("X-Content-Type-Options", "nosniff");
        res.setHeader("X-Frame-Options", "DENY");
        res.setHeader("Referrer-Policy", "no-referrer");
        if (res.getHeader("Cache-Control") == null) {
            res.setHeader("Cache-Control", "no-store");
        }
        Long startedAt = ctx.attribute(STARTED_AT);
        if (startedAt != null) {
            var millis = (System.nanoTime() - startedAt) / 1_000_000.0;
            res.setHeader("Server-Timing", String.format(Locale.ROOT, "total;dur=%.1f", millis));
        }
    }
}
