package br.com.wdc.shopping.backend;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import br.com.wdc.framework.commons.log.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.shopping.view.remote.host.spi.WebSocketConnection;
import br.com.wdc.shopping.view.remote.host.util.AppSecurity;
import br.com.wdc.shopping.view.remote.host.viewimpl.ApplicationReactImpl;
import br.com.wdc.shopping.view.remote.host.viewimpl.ApplicationReactRegistry;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsErrorContext;
import io.javalin.websocket.WsMessageContext;

/**
 * WebSocket message handler for the "/dispatcher/{id}" endpoint.
 * 
 * Manages bidirectional communication between React frontend and backend.
 * Handles session creation, message routing, and cleanup.
 */
public class DispatcherHandler {

    private static final Log LOG = Log.getLogger(DispatcherHandler.class);

    /** Custom WebSocket close code: session is invalid, client must reload the page. */
    private static final int CLOSE_SESSION_INVALID = 4001;

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.DOUBLE)
            .create();

    private static final TypeToken<Map<String, Object>> REQUEST_TYPE = 
            new TypeToken<Map<String, Object>>() {};

    private static final ConcurrentHashMap<String, String> SESSION_SIGNATURES = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, DispatcherHandler> ACTIVE_HANDLERS = new ConcurrentHashMap<>();

    private String appId;
    private String appSignature;
    private WebSocketConnection wsSession;
    private String activeWsSessionId;

    /**
     * Get or create handler for a given app ID.
     */
    public static DispatcherHandler getOrCreate(String appId) {
        return ACTIVE_HANDLERS.computeIfAbsent(appId, DispatcherHandler::new);
    }

    /**
     * Get existing handler for a given app ID, or null if not found.
     */
    public static DispatcherHandler get(String appId) {
        return ACTIVE_HANDLERS.get(appId);
    }

    /**
     * Constructor for creating a new handler (private, use getOrCreate instead).
     */
    private DispatcherHandler(String appId) {
        this.appId = appId;
    }

    /**
     * Handles WebSocket connection open.
     * Validates the session ID and establishes the connection.
     */
    public void onConnectOpen(WsConnectContext ctx) {
        try {
            // Extract session ID from path parameter
            String sessionId = ctx.pathParam("id");
            if (StringUtils.isBlank(sessionId)) {
                LOG.warn("WebSocket connection rejected: empty session ID");
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            if (!sessionId.equals(this.appId)) {
                LOG.warn("WebSocket connection rejected: session ID mismatch");
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            // Validate session ID format and signature
            var security = AppSecurity.BEAN;
            var b62 = Base62.BEAN;

            String[] appIdParts = StringUtils.split(this.appId, '.');
            if (appIdParts.length != 2) {
                LOG.warn("WebSocket connection rejected: invalid session ID format");
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            String appIdPart1 = appIdParts[0];
            String appIdPart2 = appIdParts[1];

            // Verify session ID signature
            String expectedAppIdPart2 = b62.encodeToString(
                    security.signAsHash(appIdPart1.getBytes())
            );
            
            if (!appIdPart2.equals(expectedAppIdPart2)) {
                LOG.warn("WebSocket connection rejected: invalid session ID signature");
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            // Replicates WdcStateDispatcherConfiguratator: read app_signature from
            // the HTTP upgrade request cookie (set by the frontend after key exchange).
            String signature = ctx.cookie("app_signature");
            if (StringUtils.isEmpty(signature)) {
                LOG.warn("WebSocket connection rejected: missing app_signature cookie for session: {}", this.appId);
                ctx.closeSession(CLOSE_SESSION_INVALID, "reload_required");
                return;
            }

            this.appSignature = signature;
            ctx.enableAutomaticPings(15, TimeUnit.SECONDS);
            this.activeWsSessionId = ctx.sessionId();
            this.wsSession = new JavalinWebSocketConnection(ctx);
            
            LOG.debug("WebSocket connection established for session: {} (wsId: {})", appId, this.activeWsSessionId);

            // Phase C: on reconnect, mark all views dirty (will be flushed when event -1 arrives)
            ApplicationReactImpl app = ApplicationReactRegistry.get(appId);
            if (app != null) {
                app.setWsSession(this.wsSession);
                app.resetForReconnect();
            }
            
        } catch (Exception e) {
            LOG.error("Error during WebSocket connection open", e);
            try {
                ctx.closeSession();
            } catch (Exception closeError) {
                LOG.warn("Error closing session after error", closeError);
            }
        }
    }

    /**
     * Handles incoming WebSocket messages from the client.
     * Routes messages to the appropriate ApplicationReactImpl instance.
     */
    public void onMessage(WsMessageContext ctx) {
        try {
            String jsonRequest = ctx.message();
            
            // Parse JSON request
            Map<String, Object> request = parseRequest(jsonRequest);
            if (request.isEmpty()) {
                return;
            }

            // Get or create application instance and process
            ApplicationReactImpl app = getOrCreateApp(request);
            processRequest(app, request);
            
        } catch (Exception e) {
            LOG.error("Unexpected error in WebSocket message handler", e);
        }
    }

    private Map<String, Object> parseRequest(String jsonRequest) {
        try {
            return GSON.fromJson(jsonRequest, REQUEST_TYPE.getType());
        } catch (Exception parseError) {
            LOG.warn("Failed to parse WebSocket message as JSON", parseError);
            var app = getApp();
            if (app != null) {
                app.alertUnexpectedError(LOG, parseError.getMessage(), parseError);
            }
            return Collections.emptyMap();
        }
    }

    private void processRequest(ApplicationReactImpl app, Map<String, Object> request) {
        try {
            app.setWsSession(wsSession);
            app.sendResponse(request);
        } catch (Exception processingError) {
            LOG.error("Error processing WebSocket message", processingError);
            app.alertUnexpectedError(LOG, processingError.getMessage(), processingError);
        }
    }

    /**
     * Handles WebSocket connection close.
     * Cleans up the application instance if no longer authenticated.
     */
    public void onClose(io.javalin.websocket.WsCloseContext ctx) {
        try {
            ctx.disableAutomaticPings();

            // Guard against stale close events: when a client reconnects quickly,
            // the new connection's onConnect may fire before the old connection's
            // onClose. Without this check, the old close would destroy the new
            // connection's handler and session.
            String closingWsId = ctx.sessionId();
            if (this.activeWsSessionId != null && !this.activeWsSessionId.equals(closingWsId)) {
                LOG.debug("Ignoring close for superseded WebSocket session: {} (active: {})", closingWsId, this.activeWsSessionId);
                return;
            }

            this.activeWsSessionId = null;
            this.wsSession = null;

            ApplicationReactImpl app = getApp();
            if (app != null) {
                app.setWsSession(null);
                if (!app.isAuthenticated()) {
                    LOG.debug("Releasing non-authenticated session: {}", appId);
                    app.release();
                }
            }
            LOG.debug("WebSocket connection closed for session: {}", appId);
            
            // Clean up handler and signature
            ACTIVE_HANDLERS.remove(appId);
            SESSION_SIGNATURES.remove(appId);
            
        } catch (Exception e) {
            LOG.warn("Error during WebSocket close", e);
        }
    }

    /**
     * Handles WebSocket errors.
     * Alerts the application of the error condition.
     */
    public void onError(WsErrorContext ctx) {
        try {
            ctx.disableAutomaticPings();
            Throwable error = ctx.error();
            LOG.warn("WebSocket error for session {}: {}", appId, error.getMessage(), error);
            
            ApplicationReactImpl app = getApp();
            if (app != null) {
                app.alertUnexpectedError(LOG, error.getMessage(), error);
            }
        } catch (Exception e) {
            LOG.error("Error in WebSocket error handler", e);
        }
    }

    // ============ Static Helper Methods ============

    /**
     * Register application signature for a session ID.
     * Called during initial HTTP request before WebSocket upgrade.
     */
    public static void registerSessionSignature(String appId, String signature) {
        SESSION_SIGNATURES.put(appId, signature);
        LOG.debug("Registered signature for session: {}", appId);
    }

    /**
     * Clean up signature for a session.
     * Called when session is no longer needed.
     */
    public static void unregisterSessionSignature(String appId) {
        SESSION_SIGNATURES.remove(appId);
        ACTIVE_HANDLERS.remove(appId);
        LOG.debug("Unregistered signature for session: {}", appId);
    }

    // ============ Private Helper Methods ============

    /**
     * Retrieves an existing application instance by session ID.
     */
    private ApplicationReactImpl getApp() {
        return ApplicationReactRegistry.get(appId);
    }

    /**
     * Gets or creates an application instance, initializing it with the request.
     */
    private ApplicationReactImpl getOrCreateApp(Map<String, Object> request) {
        ApplicationReactImpl app = ApplicationReactRegistry.get(appId);
        
        if (app == null) {
            // First request: include signature in request
            request.put("secret", this.appSignature);
            app = ApplicationReactRegistry.getOrCreate(appId, request);
        }
        
        // Keep the current WebSocket channel attached to the app instance.
        app.setWsSession(wsSession);
        app.extendLife();
        
        return app;
    }
}
