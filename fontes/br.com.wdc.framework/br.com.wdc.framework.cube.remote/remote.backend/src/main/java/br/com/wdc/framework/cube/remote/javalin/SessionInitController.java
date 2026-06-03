package br.com.wdc.framework.cube.remote.javalin;

import java.security.SecureRandom;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * REST endpoint for non-browser clients (desktop, mobile) to obtain a session.
 * <p>
 * Returns a JSON payload with {@code appId} (server-signed) and {@code appSKey}
 * (RSA public key) so the client can bootstrap a WebSocket connection without
 * relying on cookies from an HTML page.
 * <p>
 * Endpoint: {@code GET /<context>/api/session/init}
 */
public final class SessionInitController {

    private static final SecureRandom RND = new SecureRandom();

    private final RemoteAppSecurity security;

    public SessionInitController(RemoteAppSecurity security) {
        this.security = security;
    }

    /**
     * Registers the session-init endpoint for all context paths.
     */
    public void configure(JavalinConfig config, String... contextPaths) {
        // Root-level (no context prefix)
        config.routes.get("/api/session/init", this::handle);

        for (String ctx : contextPaths) {
            config.routes.get("/" + ctx + "/api/session/init", this::handle);
        }
    }

    private void handle(Context ctx) {
        ctx.res().setHeader("Cache-Control", "no-cache, no-store");
        ctx.res().setHeader("Pragma", "no-cache");
        ctx.res().setDateHeader("Expires", 0);

        String appId = makeAppId();
        String appSKey = security.getWebKey();

        ctx.contentType("application/json");
        ctx.result("{\"appId\":\"" + appId + "\",\"appSKey\":\"" + appSKey + "\"}");
    }

    private String makeAppId() {
        var b62 = Base62.BEAN;

        var appIdPart1Bytes = new byte[32];
        RND.nextBytes(appIdPart1Bytes);
        String appIdPart1 = b62.encodeToString(appIdPart1Bytes);

        String appIdPart2 = b62.encodeToString(security.signAsHash(appIdPart1.getBytes()));

        return appIdPart1 + "." + appIdPart2;
    }
}
