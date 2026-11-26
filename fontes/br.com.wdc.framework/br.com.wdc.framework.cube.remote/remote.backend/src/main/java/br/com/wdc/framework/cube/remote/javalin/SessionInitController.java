package br.com.wdc.framework.cube.remote.javalin;

import java.util.function.BooleanSupplier;

import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;

/**
 * REST endpoint for non-browser clients (desktop, mobile) to obtain a session.
 * <p>
 * Returns a JSON payload with {@code appId} (server-signed), so the client can open the WebSocket without relying on
 * cookies from an HTML page; the channel key is agreed on the WebSocket itself.
 * <p>
 * Endpoint: {@code GET /<context>/api/session/init}
 */
public final class SessionInitController {

    private final RemoteAppSecurity security;
    private final BooleanSupplier atCapacity;

    public SessionInitController(RemoteAppSecurity security) {
        this(security, () -> false);
    }

    public SessionInitController(RemoteAppSecurity security, BooleanSupplier atCapacity) {
        this.security = security;
        this.atCapacity = atCapacity;
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

        if (atCapacity.getAsBoolean()) {
            ctx.status(503).result("{\"error\":\"capacity_exceeded\"}");
            return;
        }

        ctx.contentType("application/json");
        ctx.result("{\"appId\":\"" + security.newSessionId() + "\"}");
    }
}
