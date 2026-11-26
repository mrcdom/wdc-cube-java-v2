package br.com.wdc.framework.cube.remote.javalin;

import java.nio.channels.ClosedChannelException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.security.ChannelCrypto;
import br.com.wdc.framework.cube.remote.CapacityExceededException;
import br.com.wdc.framework.cube.remote.RemoteAppSecurity;
import br.com.wdc.framework.cube.remote.RemoteApplication;
import br.com.wdc.framework.cube.remote.RemoteApplicationRegistry;
import br.com.wdc.framework.cube.remote.RemoteDataSecurity;
import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsCloseContext;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;

/**
 * Generic WebSocket dispatcher controller for remote applications.
 * <p>
 * Each application module creates its own instance with its own registry and security.
 * Routes are registered under a configurable context path.
 */
public final class DispatcherController {

    private static final Log LOG = Log.getLogger(DispatcherController.class);

    private static final int CLOSE_SESSION_INVALID = 4001;
    private static final int CLOSE_UNSUPPORTED_PROTOCOL = 4002;
    private static final int CLOSE_CAPACITY_EXCEEDED = 4003;

    /** The versions of the remote protocol this host speaks. */
    public static final int PROTOCOL = 1;

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.DOUBLE)
            .create();

    private static final TypeToken<Map<String, Object>> REQUEST_TYPE =
            new TypeToken<Map<String, Object>>() {};

    private final RemoteApplicationRegistry<? extends RemoteApplication> registry;
    private final RemoteAppSecurity security;

    private final ConcurrentHashMap<String, Handler> activeHandlers = new ConcurrentHashMap<>();

    public DispatcherController(RemoteApplicationRegistry<? extends RemoteApplication> registry,
                                RemoteAppSecurity security) {
        this.registry = registry;
        this.security = security;
    }

    /**
     * Registers WebSocket dispatcher routes for the given context paths.
     */
    public void configure(JavalinConfig config, String... contextPaths) {
        // Root-level dispatcher (no context prefix)
        config.routes.ws("/dispatcher/{id}", ws -> {
            ws.onConnect(this::handleConnect);
            ws.onMessage(this::handleMessage);
            ws.onClose(this::handleClose);
            ws.onError(this::handleError);
        });

        for (String ctx : contextPaths) {
            config.routes.ws("/" + ctx + "/dispatcher/{id}", ws -> {
                ws.onConnect(this::handleConnect);
                ws.onMessage(this::handleMessage);
                ws.onClose(this::handleClose);
                ws.onError(this::handleError);
            });
        }
    }

    private void handleConnect(WsConnectContext ctx) {
        try {
            String sessionId = ctx.pathParam("id");
            Handler handler = activeHandlers.computeIfAbsent(sessionId, id -> new Handler(id, security, registry));
            handler.onConnectOpen(ctx);
            LOG.debug("WebSocket dispatcher connected for session: {}", sessionId);
        } catch (Exception e) {
            LOG.error("Error during WebSocket connect", e);
            try {
                ctx.closeSession();
            } catch (Exception closeErr) {
                LOG.warn("Error closing session", closeErr);
            }
        }
    }

    private void handleMessage(WsMessageContext ctx) {
        try {
            String sessionId = ctx.pathParam("id");
            Handler handler = activeHandlers.get(sessionId);
            if (handler != null) {
                handler.onMessage(ctx);
            }
        } catch (Exception e) {
            LOG.error("Error during WebSocket message", e);
        }
    }

    private void handleClose(WsCloseContext ctx) {
        try {
            String sessionId = ctx.pathParam("id");
            Handler handler = activeHandlers.get(sessionId);
            if (handler != null) {
                handler.onClose(ctx, activeHandlers);
            }
        } catch (Exception e) {
            LOG.warn("Error during WebSocket close", e);
        }
    }

    private void handleError(WsErrorContext ctx) {
        try {
            String sessionId = ctx.pathParam("id");
            Handler handler = activeHandlers.get(sessionId);
            if (handler != null) {
                handler.onError(ctx);
            }
        } catch (Exception e) {
            LOG.error("Error in WebSocket error handler", e);
        }
    }

    // :: Inner handler per session

    private static final class Handler {

        private final String appId;
        private final RemoteAppSecurity security;
        private final RemoteApplicationRegistry<? extends RemoteApplication> registry;

        /** The key the current connection agreed on; {@code null} until its handshake. */
        private byte[] channelKey;
        private WsContext wsSession;
        private String activeWsSessionId;

        Handler(String appId, RemoteAppSecurity security,
                RemoteApplicationRegistry<? extends RemoteApplication> registry) {
            this.appId = appId;
            this.security = security;
            this.registry = registry;
        }

        void onConnectOpen(WsConnectContext ctx) {
            try {
                String sessionId = ctx.pathParam("id");
                if (StringUtils.isBlank(sessionId) || !sessionId.equals(this.appId)) {
                    LOG.warn("WebSocket connection rejected: session ID mismatch");
                    ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                    return;
                }

                if (!security.isSessionId(this.appId)) {
                    LOG.warn("WebSocket connection rejected: invalid session ID signature");
                    ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                    return;
                }

                // A new connection agrees on its own key in its first message
                this.channelKey = null;

                ctx.enableAutomaticPings(15, TimeUnit.SECONDS);
                this.activeWsSessionId = ctx.sessionId();
                this.wsSession = ctx;

                LOG.debug("WebSocket connection established for session: {} (wsId: {})", appId, this.activeWsSessionId);

            } catch (Exception e) {
                LOG.error("Error during WebSocket connection open", e);
                try {
                    ctx.closeSession();
                } catch (Exception closeError) {
                    LOG.warn("Error closing session after error", closeError);
                }
            }
        }

        void onMessage(WsMessageContext ctx) {
            try {
                String jsonRequest = ctx.message();

                if (!ctx.sessionId().equals(this.activeWsSessionId)) {
                    return; // a connection a newer one replaced
                }

                Map<String, Object> request = parseRequest(jsonRequest);
                if (request.isEmpty()) {
                    return;
                }

                if (this.channelKey == null) {
                    handshake(ctx, request);
                    return;
                }

                RemoteApplication app = getOrCreateApp(request);
                processRequest(app, request);

            } catch (CapacityExceededException e) {
                LOG.warn("WebSocket rejected (capacity exceeded) for session: {}", this.appId);
                try {
                    ctx.closeSession(CLOSE_CAPACITY_EXCEEDED, "capacity_exceeded");
                } catch (Exception closeErr) {
                    LOG.warn("Error closing session after capacity rejection", closeErr);
                }
            } catch (Exception e) {
                LOG.error("Unexpected error in WebSocket message handler", e);
            }
        }

        void onClose(WsCloseContext ctx, ConcurrentHashMap<String, Handler> handlers) {
            try {
                ctx.disableAutomaticPings();

                String closingWsId = ctx.sessionId();
                if (this.activeWsSessionId != null && !this.activeWsSessionId.equals(closingWsId)) {
                    LOG.debug("Ignoring close for superseded WebSocket session: {} (active: {})", closingWsId, this.activeWsSessionId);
                    return;
                }

                this.activeWsSessionId = null;
                this.wsSession = null;

                RemoteApplication app = registry.get(appId);
                if (app != null) {
                    app.setWsSession(null);
                    if (!app.isAuthenticated() || registry.isImmediateRelease()) {
                        LOG.debug("Releasing session on disconnect: {} (authenticated={}, immediateRelease={})",
                                appId, app.isAuthenticated(), registry.isImmediateRelease());
                        app.release();
                    }
                }
                LOG.debug("WebSocket connection closed for session: {}", appId);

                handlers.remove(appId);

            } catch (Exception e) {
                LOG.warn("Error during WebSocket close", e);
            }
        }

        void onError(WsErrorContext ctx) {
            try {
                ctx.disableAutomaticPings();
                Throwable error = ctx.error();

                // ClosedChannelException is routine — the client disconnected abruptly.
                // Log at DEBUG to avoid polluting the log with expected network events.
                if (error instanceof ClosedChannelException) {
                    LOG.debug("WebSocket closed abruptly for session {}", appId);
                    return;
                }

                LOG.warn("WebSocket error for session {}: {}", appId, error.getMessage(), error);

                RemoteApplication app = registry.get(appId);
                if (app != null) {
                    app.alertUnexpectedError(LOG, error.getMessage(), error);
                }
            } catch (Exception e) {
                LOG.error("Error in WebSocket error handler", e);
            }
        }

        private Map<String, Object> parseRequest(String jsonRequest) {
            try {
                return GSON.fromJson(jsonRequest, REQUEST_TYPE.getType());
            } catch (Exception parseError) {
                LOG.warn("Failed to parse WebSocket message as JSON", parseError);
                var app = registry.get(appId);
                if (app != null) {
                    app.alertUnexpectedError(LOG, parseError.getMessage(), parseError);
                }
                return Collections.emptyMap();
            }
        }

        /**
         * The first message of a connection (protocol 1): {@code {"protocol": 1, "clientKey": "<P-256 key>"}}. The
         * answer carries the host's key, and both ends derive the connection's key. A message with another version,
         * or none, is answered with {@code unsupported-protocol}, and the connection is closed.
         */
        private void handshake(WsMessageContext ctx, Map<String, Object> request) {
            var protocol = request.get("protocol") instanceof Number n ? n.intValue() : null;
            var clientKey = request.get("clientKey") instanceof String k ? k : null;
            if (protocol == null || protocol != PROTOCOL || clientKey == null) {
                LOG.warn("WebSocket refused: unsupported protocol {} for session: {}", protocol, this.appId);
                ctx.send("{\"error\":\"unsupported-protocol\",\"protocols\":[" + PROTOCOL + "]}");
                ctx.closeSession(CLOSE_UNSUPPORTED_PROTOCOL, "unsupported_protocol");
                return;
            }

            byte[] key;
            String serverKey;
            try {
                var peer = ChannelCrypto.decodePublicKey(clientKey);
                var own = ChannelCrypto.newKeyPair();
                key = ChannelCrypto.deriveKey(own.getPrivate(), peer, this.appId);
                serverKey = ChannelCrypto.encodePublicKey(own.getPublic());
            } catch (IllegalArgumentException invalidKey) {
                LOG.warn("WebSocket refused: invalid client key for session: {}", this.appId);
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            this.channelKey = key;
            ctx.send("{\"protocol\":" + PROTOCOL + ",\"serverKey\":\"" + serverKey + "\"}");

            RemoteApplication app = registry.get(appId);
            if (app != null) {
                app.getDataSecurity().useKey(key);
                app.setWsSession(this.wsSession);
                app.resetForReconnect();
            }
        }

        private RemoteApplication getOrCreateApp(Map<String, Object> request) {
            RemoteApplication app = registry.get(appId);

            if (app == null) {
                request.put(RemoteDataSecurity.CHANNEL_KEY, this.channelKey);
                app = registry.getOrCreate(appId, request);
                app.getDataSecurity().useKey(this.channelKey);
            }

            app.setWsSession(wsSession);
            app.extendLife();

            return app;
        }

        private void processRequest(RemoteApplication app, Map<String, Object> request) {
            try {
                app.setWsSession(wsSession);
                app.sendResponse(request);
            } catch (Exception processingError) {
                LOG.error("Error processing WebSocket message", processingError);
                app.alertUnexpectedError(LOG, processingError.getMessage(), processingError);
            }
        }
    }
}
