package br.com.wdc.shopping.view.react;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.shopping.view.react.skeleton.spi.WebSocketConnection;
import br.com.wdc.shopping.view.react.skeleton.util.AppSecurity;
import br.com.wdc.shopping.view.react.skeleton.viewimpl.ApplicationReactImpl;
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

    private static final Logger LOG = LoggerFactory.getLogger(DispatcherHandler.class);

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

    /**
     * Get or create handler for a given app ID.
     */
    public static DispatcherHandler getOrCreate(String appId) {
        return ACTIVE_HANDLERS.computeIfAbsent(appId, id -> new DispatcherHandler(id));
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
                ctx.closeSession();
                return;
            }

            if (!sessionId.equals(this.appId)) {
                LOG.warn("WebSocket connection rejected: session ID mismatch");
                ctx.closeSession();
                return;
            }

            // Validate session ID format and signature
            var security = AppSecurity.BEAN;
            var b62 = Base62.BEAN;

            String[] appIdParts = StringUtils.split(this.appId, '.');
            if (appIdParts.length != 2) {
                LOG.warn("WebSocket connection rejected: invalid session ID format");
                ctx.closeSession();
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
                ctx.closeSession();
                return;
            }

            // Replicates WdcStateDispatcherConfiguratator: read app_signature from
            // the HTTP upgrade request cookie (set by the frontend after key exchange).
            String signature = ctx.cookie("app_signature");
            if (StringUtils.isEmpty(signature)) {
                LOG.warn("WebSocket connection rejected: missing app_signature cookie for session: {}", this.appId);
                ctx.closeSession();
                return;
            }

            this.appSignature = signature;
            ctx.enableAutomaticPings(15, TimeUnit.SECONDS);
            this.wsSession = new JavalinWebSocketConnection(ctx);
            
            LOG.debug("WebSocket connection established for session: {}", appId);
            
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
            Map<String, Object> request;
            try {
                request = GSON.fromJson(jsonRequest, REQUEST_TYPE.getType());
            } catch (Exception parseError) {
                LOG.warn("Failed to parse WebSocket message as JSON", parseError);
                var app = getApp();
                if (app != null) {
                    app.alertUnexpectedError(LOG, parseError.getMessage(), parseError);
                }
                return;
            }

            // Get or create application instance
            ApplicationReactImpl app = getOrCreateApp(request);
            try {
                app.setWsSession(wsSession);
                
                // Process the request and send response
                app.sendResponse(request);
                
            } catch (Exception processingError) {
                LOG.error("Error processing WebSocket message", processingError);
                app.alertUnexpectedError(LOG, processingError.getMessage(), processingError);
            }
            
        } catch (Exception e) {
            LOG.error("Unexpected error in WebSocket message handler", e);
        }
    }

    /**
     * Handles WebSocket connection close.
     * Cleans up the application instance if no longer authenticated.
     */
    public void onClose(io.javalin.websocket.WsCloseContext ctx) {
        try {
            ctx.disableAutomaticPings();
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
        return ApplicationReactImpl.get(appId);
    }

    /**
     * Gets or creates an application instance, initializing it with the request.
     */
    private ApplicationReactImpl getOrCreateApp(Map<String, Object> request) {
        ApplicationReactImpl app = ApplicationReactImpl.get(appId);
        
        if (app == null) {
            // First request: include signature in request
            request.put("secret", this.appSignature);
            app = ApplicationReactImpl.getOrCreate(appId, request);
        }
        
        // Keep the current WebSocket channel attached to the app instance.
        app.setWsSession(wsSession);
        app.extendLife();
        
        return app;
    }
}
