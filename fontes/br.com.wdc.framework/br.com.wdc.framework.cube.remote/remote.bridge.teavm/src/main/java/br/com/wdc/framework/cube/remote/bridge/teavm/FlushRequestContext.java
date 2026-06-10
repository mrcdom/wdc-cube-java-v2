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

/**
 * Gerencia a conexão WebSocket, fila de requests, keepalive e estado de submitting.
 */
public class FlushRequestContext {

    private static final int KEEP_ALIVE_INTERVAL = 15_000;

    private final ViewStateCoordinator app;

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
    private String pendingSecret;
    Map<String, Object> pendingStorage;

    public FlushRequestContext(ViewStateCoordinator app) {
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
        if (socket == null || !isSocketOpen(socket)) {
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

        if (hasData || pendingSecret != null || pendingStorage != null) {
            requestObj.put("event", allEvents);
            if (pendingSecret != null) {
                requestObj.put("secret", pendingSecret);
                pendingSecret = null;
            }
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
        if (socket != null && isSocketOpenOrConnecting(socket)) {
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

    // -- WebSocket event handlers --

    private void handleOpen() {
        Console.log("WebSocket connected");
        app.isConnected = true;
        pendingSecret = SecurityBoot.getSignature();
        initKeepAliveChecks();
        // Build bootstrap storage (async cipher callbacks), then flush.
        // Nothing is sent until all ciphers resolve.
        app.buildBootstrapStorage(this::flush);
    }

    private void handleClose(int code) {
        Console.warn("WebSocket closed: " + code);
        if (code == 4001) {
            reload();
            return;
        }
        handleDisconnect("WebSocket closed: " + code);
    }

    private void handleError() {
        Console.error("WebSocket error");
        handleDisconnect("WebSocket error");
    }

    private void handleDisconnect(String cause) {
        socket = null;
        app.isConnected = false;
        stopKeepAliveChecks();
        userRequestIds.clear();
        setSubmitting(false);
        app.reconnectController.reconnect(cause);
    }

    @SuppressWarnings("unchecked")
    private void handleMessage(String data) {
        if (app.reconnectController.count > 0) {
            app.reconnectController.reset();
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
        }

        Object states = response.get("states");
        if (states instanceof List<?> list) {
            app.applyViewStates(list);
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
        if (socket != null && isSocketOpen(socket)) {
            pendingKeepAlive = Timers.setTimeout((Timers.TimerCallback) () -> {
                pendingKeepAlive = 0;
                persistState();
                sendToSocket(socket, "{\"ping\":true}");
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
        var scope = app.viewMap.get(ViewStateCoordinator.BROWSER_VSID);
        if (scope != null) {
            var state = scope.getState();
            Object current = state.get("submitting");
            boolean currentVal = current instanceof Boolean b && b;
            if (currentVal != value) {
                state.put("submitting", value);
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

    @JSBody(params = {}, script = "location.reload();")
    private static native void reload();

    @JSBody(params = {"href"}, script = "window.location.href = href;")
    private static native void setLocationHref(String href);
}
