package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Console;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsIntConsumer;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsRunnable;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.JsStringConsumer;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.Timers;
import br.com.wdc.framework.cube.remote.bridge.teavm.interop.WebCrypto;

/**
 * Manages the WebSocket connection, request queue, keepalive and submitting state.
 * <p>
 * Every connection, the first one and each reconnection, starts with the handshake of protocol version
 * {@value Handshake#PROTOCOL} (docs/bridge-skeleton-protocol.md §6): the first and only message is the client's
 * ephemeral P-256 key; once the host answers with its own, both derive the connection key ({@link DataSecurity}), and
 * only then go out the bootstrap message (with the client storage ciphered with that key), the requests that waited,
 * and the keep-alive pings. A host that refuses the version leaves the page out of date: no reconnection follows.
 */
public class FlushRequestContext {

    private static final int KEEP_ALIVE_INTERVAL = 15_000;

    private static final int CLOSE_RELOAD_REQUIRED = 4001;
    private static final int CLOSE_UNSUPPORTED_PROTOCOL = 4002;

    /** Where the current connection is (docs/bridge-skeleton-protocol.md §7.3). */
    private enum Phase {
        /** No connection. */
        CLOSED,
        /** The client's key went out (or is being made); waiting for the host's. */
        HANDSHAKE,
        /** The host answered: deriving the connection key and ciphering the bootstrap storage. */
        KEYING,
        /** The bootstrap message went out; requests flow. */
        CONNECTED,
        /** The host refused the protocol version: the page is out of date, and it does not reconnect. */
        OUTDATED
    }

    private final ScopeCoordinator app;

    private JSObject socket;
    private final Map<Integer, Map<String, Object>> requestMap = new LinkedHashMap<>();
    private final Set<Integer> userRequestIds = new HashSet<>();

    int lastSentRequestId = -1;
    int requestCount = 0;
    int lastProcessedId = -1;
    private int keepAliveHandler = 0;
    private int pendingKeepAlive = 0;
    private int submittingTimer = 0;
    private int submittingTimeout = 0;
    private Phase phase = Phase.CLOSED;
    private JSObject keyPair;
    private final List<String> heldMessages = new ArrayList<>();
    private boolean bootstrapPending;
    Map<String, Object> pendingStorage;

    public FlushRequestContext(ScopeCoordinator app) {
        this.app = app;

        String savedSeq = getSessionItem("req_seq");
        if (savedSeq != null && !savedSeq.isEmpty()) {
            try {
                int parsed = Integer.parseInt(savedSeq);
                if (parsed > 0) {
                    this.requestCount = parsed;
                }
            } catch (NumberFormatException ignored) {
                // cookie value was not a valid integer — start from zero
            }
        }
    }

    public void submit(Map<String, Object> formMap, String vsid, int eventId, boolean silent) {
        cancelPendingKeepAlive();

        formMap.put("requestId", requestCount);
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) formMap.get("event");
        if (events == null) {
            events = new ArrayList<>();
            formMap.put("event", events);
        }
        events.add(vsid + ":" + eventId);

        requestMap.put(requestCount, formMap);
        if (!silent) {
            userRequestIds.add(requestCount);
        }
        requestCount++;
        persistState();

        resetKeepAliveTimer();
        flush();
    }

    @SuppressWarnings("unchecked")
    public void flush() {
        if (phase != Phase.CONNECTED || socket == null || !isSocketOpen(socket)) {
            return;
        }

        Map<String, Object> requestObj = new LinkedHashMap<>();
        List<String> allEvents = new ArrayList<>();
        boolean hasData = false;

        for (int i = lastSentRequestId + 1; i < requestCount; i++) {
            Map<String, Object> requestItemObj = requestMap.get(i);
            if (requestItemObj == null) {
                continue;
            }

            for (Map.Entry<String, Object> entry : requestItemObj.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (value == null) continue;

                if ("event".equals(key)) {
                    List<String> valueArray = (List<String>) value;
                    allEvents.addAll(valueArray);
                } else if ("requestId".equals(key)) {
                    // will be set below
                } else {
                    Map<String, Object> formData = (Map<String, Object>) requestObj.get(key);
                    if (formData == null) {
                        formData = new LinkedHashMap<>();
                        requestObj.put(key, formData);
                    }
                    if (value instanceof Map<?, ?> valueMap) {
                        formData.putAll((Map<String, Object>) valueMap);
                    } else {
                        requestObj.put(key, value);
                    }
                }
            }

            requestObj.put("requestId", i);
            lastSentRequestId = i;
            hasData = true;
        }

        if (hasData || bootstrapPending || pendingStorage != null) {
            requestObj.put("event", allEvents);
            bootstrapPending = false;
            if (pendingStorage != null) {
                requestObj.put("storage", pendingStorage);
                pendingStorage = null;
            }
            String json = JsonParser.stringify(requestObj);
            sendToSocket(socket, json);
            if (!userRequestIds.isEmpty()) {
                setSubmitting(true);
            }
        }
    }

    public void open(String url) {
        if (phase == Phase.OUTDATED || socket != null && isSocketOpenOrConnecting(socket)) {
            return;
        }

        Console.log("WebSocket opening: " + url);

        socket = createWebSocket(url);
        setupWebSocket(socket,
                (JsRunnable) this::handleOpen,
                (JsIntConsumer) this::handleClose,
                (JsRunnable) this::handleError,
                (JsStringConsumer) this::handleMessage);
    }

    public void close() {
        if (socket != null) {
            closeSocket(socket);
            socket = null;
        }
    }

    /** Whether the host refused this client's protocol version (the page is out of date). */
    public boolean isOutdated() {
        return phase == Phase.OUTDATED;
    }

    // -- WebSocket event handlers --

    private void handleOpen() {
        Console.log("WebSocket connected; handshake (protocol " + Handshake.PROTOCOL + ")");
        var opened = socket;
        phase = Phase.HANDSHAKE;
        DataSecurity.forgetKey();
        WebCrypto.newKeyPair((pair, clientKey) -> {
            if (opened != socket || phase != Phase.HANDSHAKE) {
                return;
            }
            keyPair = pair;
            sendToSocket(opened, JsonParser.stringify(Handshake.hello(clientKey)));
        }, reason -> failHandshake(opened, reason));
    }

    /** The first message from the host on a connection: the answer to the handshake. */
    private void handleHandshakeAnswer(String data) {
        var opened = socket;
        var answer = JsonParser.parseObject(data);
        switch (Handshake.read(answer)) {
            case REFUSED -> outdated();
            case ACCEPTED -> {
                var pair = keyPair;
                keyPair = null;
                if (pair == null) {
                    failHandshake(opened, "an answer before the handshake");
                    return;
                }
                phase = Phase.KEYING;
                WebCrypto.deriveKey(pair, Handshake.serverKey(answer), app.id,
                        key -> handleConnectionKey(opened, key),
                        reason -> failHandshake(opened, reason));
            }
            case UNEXPECTED -> failHandshake(opened, "not an answer to the handshake: " + data);
        }
    }

    /** The connection agreed on its key: the bootstrap message goes out, then the requests that waited. */
    private void handleConnectionKey(JSObject opened, JSObject key) {
        if (opened != socket || phase != Phase.KEYING) {
            return;
        }
        DataSecurity.useKey(key);
        app.buildBootstrapStorage(() -> {
            if (opened != socket || phase != Phase.KEYING) {
                return;
            }
            phase = Phase.CONNECTED;
            app.isConnected = true;
            if (app.reconnectController.count > 0) {
                app.reconnectController.reset();
            }
            initKeepAliveChecks();
            bootstrapPending = true;
            flush();

            var held = new ArrayList<>(heldMessages);
            heldMessages.clear();
            for (var message : held) {
                handleMessage(message);
            }
        });
    }

    private void failHandshake(JSObject opened, String reason) {
        Console.error("[FlushRequestContext] handshake failed: " + reason);
        if (opened == socket && opened != null) {
            closeSocket(opened);
        }
    }

    /** The host does not speak this client's protocol version: no reconnection; the user is told to reload. */
    private void outdated() {
        Console.warn("[FlushRequestContext] the host refused protocol " + Handshake.PROTOCOL + ": the page is out of date");
        phase = Phase.OUTDATED;
        var closing = socket;
        socket = null;
        if (closing != null) {
            closeSocket(closing);
        }
        keyPair = null;
        heldMessages.clear();
        pendingStorage = null;
        bootstrapPending = false;
        DataSecurity.forgetKey();
        app.isConnected = false;
        stopKeepAliveChecks();
        cancelPendingKeepAlive();
        userRequestIds.clear();
        setSubmitting(false);
        app.reconnectController.close();
        app.showOutdated();
    }

    private void handleClose(int code) {
        Console.warn("WebSocket closed: " + code);
        if (phase == Phase.OUTDATED) {
            return;
        }
        if (code == CLOSE_UNSUPPORTED_PROTOCOL) {
            outdated();
            return;
        }
        if (code == CLOSE_RELOAD_REQUIRED) {
            // Discard the stored app_id before reloading — otherwise the reload
            // reconnects with the same invalid id and enters an infinite loop.
            removeSessionItem("app_id");
            reload();
            return;
        }
        handleDisconnect("WebSocket closed: " + code);
    }

    private void handleError() {
        Console.error("WebSocket error");
        if (phase == Phase.OUTDATED) {
            return;
        }
        handleDisconnect("WebSocket error");
    }

    private void handleDisconnect(String cause) {
        socket = null;
        phase = Phase.CLOSED;
        keyPair = null;
        heldMessages.clear();
        pendingStorage = null;
        bootstrapPending = false;
        DataSecurity.forgetKey();
        app.isConnected = false;
        stopKeepAliveChecks();
        userRequestIds.clear();
        setSubmitting(false);
        app.reconnectController.reconnect(cause);
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(String data) {
        switch (phase) {
            case HANDSHAKE -> {
                handleHandshakeAnswer(data);
                return;
            }
            case KEYING -> {
                heldMessages.add(data);
                return;
            }
            case CLOSED, OUTDATED -> {
                return;
            }
            case CONNECTED -> {
                // handled below
            }
        }

        Map<String, Object> response = JsonParser.parseObject(data);
        if (response.isEmpty()) return;

        Object released = response.get("releasedViews");
        if (released instanceof List<?> releasedList) {
            app.viewGarbageCollector.release((List<String>) releasedList);
        }

        Object active = response.get("activeViews");
        if (active instanceof List<?> activeList) {
            app.viewGarbageCollector.sweep((List<String>) activeList);
        }

        Object reqId = response.get("requestId");
        if (reqId instanceof Number n) {
            int processedId = n.intValue();
            for (int i = lastProcessedId + 1; i <= processedId; i++) {
                requestMap.remove(i);
                userRequestIds.remove(i);
                lastProcessedId = i;
            }
        }

        Object uri = response.get("uri");
        if (uri instanceof String u) {
            app.path = u;
            setLocationHref("#" + u);
            app.persistUri(u);
        }

        Object scopes = response.get("scopes");
        if (scopes instanceof List<?> list) {
            app.applyScopes(list);
        }

        Object storage = response.get("storage");
        if (storage instanceof Map<?, ?> storageDelta) {
            app.applyStorageDelta((Map<String, Object>) storageDelta);
        }

        flush();
        setSubmitting(!userRequestIds.isEmpty());
    }

    // -- Keep alive --

    private void initKeepAliveChecks() {
        stopKeepAliveChecks();
        keepAliveHandler = Timers.setTimeout((Timers.TimerCallback) this::keepAlive, KEEP_ALIVE_INTERVAL);
    }

    private void stopKeepAliveChecks() {
        if (keepAliveHandler != 0) {
            Timers.clearTimeout(keepAliveHandler);
            keepAliveHandler = 0;
        }
    }

    private void keepAliveNow() {
        cancelPendingKeepAlive();
        if (phase == Phase.CONNECTED && socket != null && isSocketOpen(socket)) {
            pendingKeepAlive = Timers.setTimeout((Timers.TimerCallback) () -> {
                pendingKeepAlive = 0;
                persistState();
                if (phase == Phase.CONNECTED && socket != null && isSocketOpen(socket)) {
                    sendToSocket(socket, "{\"ping\":true}");
                }
            }, 80);
        }
    }

    private void cancelPendingKeepAlive() {
        if (pendingKeepAlive != 0) {
            Timers.clearTimeout(pendingKeepAlive);
            pendingKeepAlive = 0;
        }
    }

    private void resetKeepAliveTimer() {
        if (keepAliveHandler != 0) {
            stopKeepAliveChecks();
            keepAliveHandler = Timers.setTimeout((Timers.TimerCallback) this::keepAlive, KEEP_ALIVE_INTERVAL);
        }
    }

    private void keepAlive() {
        stopKeepAliveChecks();
        keepAliveNow();
        keepAliveHandler = Timers.setTimeout((Timers.TimerCallback) this::keepAlive, KEEP_ALIVE_INTERVAL);
    }

    // -- Submitting state --

    private void setSubmitting(boolean value) {
        if (value) {
            if (submittingTimer == 0) {
                submittingTimer = Timers.setTimeout((Timers.TimerCallback) () -> {
                    submittingTimer = 0;
                    applySubmitting(true);
                }, 200);
            }
            if (submittingTimeout == 0) {
                submittingTimeout = Timers.setTimeout((Timers.TimerCallback) () -> {
                    submittingTimeout = 0;
                    setSubmitting(false);
                }, 15_000);
            }
        } else {
            if (submittingTimer != 0) {
                Timers.clearTimeout(submittingTimer);
                submittingTimer = 0;
            }
            if (submittingTimeout != 0) {
                Timers.clearTimeout(submittingTimeout);
                submittingTimeout = 0;
            }
            applySubmitting(false);
        }
    }

    private void applySubmitting(boolean value) {
        var scope = app.viewMap.get(ScopeCoordinator.BROWSER_VSID);
        if (scope != null) {
            var values = scope.getValues();
            Object current = values.get("submitting");
            boolean currentVal = current instanceof Boolean b && b;
            if (currentVal != value) {
                values.put("submitting", value);
                scope.forceUpdate();
            }
        }
    }

    private void persistState() {
        setSessionItem("req_seq", String.valueOf(requestCount));
    }

    // -- JS interop --

    @JSBody(params = {"url"}, script = ""
            + "var s = new WebSocket(url, ['wdc']);"
            + "s.withCredentials = true;"
            + "return s;")
    private static native JSObject createWebSocket(String url);

    @JSBody(params = {"ws", "onOpen", "onClose", "onError", "onMessage"}, script = ""
            + "ws.onopen = onOpen;"
            + "ws.onclose = function(e) { onClose(e.code); };"
            + "ws.onerror = onError;"
            + "ws.onmessage = function(e) { onMessage(e.data); };")
    private static native void setupWebSocket(JSObject ws, JsRunnable onOpen, JsIntConsumer onClose, JsRunnable onError, JsStringConsumer onMessage);

    @JSBody(params = {"ws", "data"}, script = "ws.send(data);")
    private static native void sendToSocket(JSObject ws, String data);

    @JSBody(params = {"ws"}, script = "ws.close();")
    private static native void closeSocket(JSObject ws);

    @JSBody(params = {"ws"}, script = "return ws.readyState === 1;")
    private static native boolean isSocketOpen(JSObject ws);

    @JSBody(params = {"ws"}, script = "return ws.readyState === 0 || ws.readyState === 1;")
    private static native boolean isSocketOpenOrConnecting(JSObject ws);

    @JSBody(params = {"key"}, script = "try { return sessionStorage.getItem(key); } catch(e) { return null; }")
    private static native String getSessionItem(String key);

    @JSBody(params = {"key", "val"}, script = "try { sessionStorage.setItem(key, val); } catch(e) {}")
    private static native void setSessionItem(String key, String val);

    @JSBody(params = {"key"}, script = "try { sessionStorage.removeItem(key); } catch(e) {}")
    private static native void removeSessionItem(String key);

    @JSBody(params = {}, script = "location.reload();")
    private static native void reload();

    @JSBody(params = {"href"}, script = "window.location.href = href;")
    private static native void setLocationHref(String href);
}
