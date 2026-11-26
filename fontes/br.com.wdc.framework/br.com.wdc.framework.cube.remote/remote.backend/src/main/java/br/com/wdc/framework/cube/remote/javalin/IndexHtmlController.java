package br.com.wdc.framework.cube.remote.javalin;


import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import jakarta.servlet.http.Cookie;

/**
 * Handles session cookie generation for the SPA entrypoint (index.html).
 * <p>
 * Plants the {@code app_id} cookie (a signed random session id) so the client gets a valid, server-signed session id on
 * the very first page load. The channel key is agreed on the WebSocket, per connection.
 * <p>
 * Each application module creates its own instance with its own security.
 */
public final class IndexHtmlController {

    private final RemoteAppSecurity security;

    public IndexHtmlController(RemoteAppSecurity security) {
        this.security = security;
    }

    /**
     * Registers the session cookie handler before index.html for all context paths.
     */
    public void configure(JavalinConfig config, String... contextPaths) {
        for (String ctx : contextPaths) {
            config.routes.before("/" + ctx + "/index.html", this::handle);
        }
    }

    private void handle(Context ctx) {
        ctx.res().setHeader("Cache-Control", "no-cache, no-store");
        ctx.res().setHeader("Pragma", "no-cache");
        ctx.res().setDateHeader("Expires", 0);

        // Secure attribute only when the request arrives over HTTPS (directly or via proxy,
        // read from nginx's X-Forwarded-Proto). On plain HTTP (e.g. QA without TLS), browsers
        // REJECT Secure cookies (Firefox always) — without the app_id the WS handshake falls into
        // a 4001/reload loop.
        boolean secure = isHttps(ctx);

        Cookie appIdCookie = new Cookie("app_id", security.newSessionId());
        appIdCookie.setPath("/");
        appIdCookie.setMaxAge(10);
        appIdCookie.setSecure(secure);
        ctx.res().addCookie(appIdCookie);
    }

    private static boolean isHttps(Context ctx) {
        var forwardedProto = ctx.header("X-Forwarded-Proto");
        if (forwardedProto != null && !forwardedProto.isBlank()) {
            return "https".equalsIgnoreCase(forwardedProto.trim());
        }
        return "https".equalsIgnoreCase(ctx.scheme());
    }
}
