package br.com.wdc.shopping.backend.controller;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import br.com.wdc.framework.commons.log.Log;
import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsContext;

/**
 * Development-only live-reload support for TeaVM frontends.
 * <p>
 * Provides two mechanisms:
 * <ul>
 *   <li><b>WebSocket</b> at {@code /__dev/ws} — browser connects to receive reload notifications</li>
 *   <li><b>HTTP POST</b> at {@code /__dev/notify?context=<name>} — watch script calls after build</li>
 * </ul>
 * <p>
 * Uses a monotonic version counter so clients that reconnect after a missed notification
 * can detect the gap and reload immediately.
 */
public class DevReloadController {

    private static final Log LOG = Log.getLogger(DevReloadController.class);

    private static final DevReloadController INSTANCE = new DevReloadController();

    private final Set<WsContext> clients = ConcurrentHashMap.newKeySet();
    private final Map<String, AtomicLong> versions = new ConcurrentHashMap<>();

    private DevReloadController() {
    }

    public static void configure(JavalinConfig config) {
        // WebSocket endpoint for browser clients
        config.routes.ws("/__dev/ws", ws -> {
            ws.onConnect(ctx -> {
                INSTANCE.clients.add(ctx);
                // Send current version so client can detect missed notifications
                String context = ctx.queryParam("context");
                long version = INSTANCE.getVersion(context);
                ctx.send("{\"type\":\"version\",\"context\":\"" + (context != null ? context : "*")
                        + "\",\"version\":" + version + "}");
                LOG.info("[DevReload] Browser connected (total: {}), sent version={} for context={}",
                        INSTANCE.clients.size(), version, context);
            });
            ws.onClose(ctx -> {
                INSTANCE.clients.remove(ctx);
                LOG.debug("[DevReload] Browser disconnected (total: {})", INSTANCE.clients.size());
            });
            ws.onError(ctx -> {
                INSTANCE.clients.remove(ctx);
            });
        });

        // HTTP endpoint for watch scripts to trigger reload
        config.routes.post("/__dev/notify", ctx -> {
            String context = ctx.queryParam("context");
            long version = INSTANCE.incrementVersion(context);
            INSTANCE.notifyBrowsers(context, version);
            ctx.json(Map.of("status", "ok", "context", context != null ? context : "*", "version", version));
        });

        LOG.info("[DevReload] Dev reload endpoints active: ws=/__dev/ws, notify=POST /__dev/notify?context=<name>");
    }

    private long getVersion(String context) {
        String key = context != null ? context : "*";
        return versions.computeIfAbsent(key, k -> new AtomicLong(0)).get();
    }

    private long incrementVersion(String context) {
        String key = context != null ? context : "*";
        return versions.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    /**
     * Notifies all connected browsers to reload.
     */
    private void notifyBrowsers(String context, long version) {
        String message = "{\"type\":\"reload\",\"context\":\"" + (context != null ? context : "*")
                + "\",\"version\":" + version + "}";
        int sent = 0;
        for (WsContext client : clients) {
            try {
                client.send(message);
                sent++;
            } catch (Exception e) {
                clients.remove(client);
            }
        }
        LOG.info("[DevReload] Reload notification sent to {} browser(s) [context={}, version={}]", sent,
                context != null ? context : "*", version);
    }

    /**
     * Disconnects all browser clients (called on server shutdown).
     */
    public static void stop() {
        INSTANCE.clients.clear();
    }
}
