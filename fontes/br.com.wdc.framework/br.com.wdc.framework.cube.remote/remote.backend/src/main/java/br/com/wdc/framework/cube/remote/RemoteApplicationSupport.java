package br.com.wdc.framework.cube.remote;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.commons.gson.JsonExtensibleObjectOutput;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubeIntent;
import io.javalin.websocket.WsContext;

/**
 * Reusable implementation of the {@link RemoteApplication} contract.
 * <p>
 * Concrete application classes (e.g., ShoppingApplicationImpl) delegate
 * all remote infrastructure to an instance of this class.
 * <p>
 * This class manages: WebSocket session, dirty view tracking, flush logic,
 * dispatch/response phases, browser presenter, data security, and lifecycle.
 */
public class RemoteApplicationSupport {

    /**
     * Callback interface for host-specific operations that the support
     * class cannot implement generically.
     */
    public interface Host {
        CubeApplication getCubeApp();

        @SuppressWarnings("java:S1452")
        RemoteApplicationRegistry<?> getRegistry();

        RemoteAppSecurity getAppSecurity();

        void performGo(CubeIntent intent);

        boolean isAuthenticated();
    }

    private static final Log LOG = Log.getLogger(RemoteApplicationSupport.class);

    public static final Duration DEFAULT_TIME_SPAN = Duration.ofMinutes(3);
    private static final long ACTIVE_VIEWS_INTERVAL = 5 * 60 * 1000L; // 5 minutes

    // :: State

    private final String id;
    private final Host host;
    private final RemoteAppSecurity security;

    private Duration timeSpan = DEFAULT_TIME_SPAN;
    private long expireMoment;
    private RemoteDataSecurity dataSecurity;
    private transient @SuppressWarnings("java:S2065") WsContext wsSession;

    private RemoteBrowserPresenter browserPresenter;
    private final Map<String, RemoteViewImpl> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, RemoteViewImpl> viewMap = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> releasedViews = new ConcurrentLinkedQueue<>();
    private long lastRequestId = -1;
    private long lastActiveViewsSentAt;
    private String lastSentFragment;
    private boolean historyDirty;
    private boolean navigationAttempted;
    private int instanceIdGen = 1;

    private final ConcurrentLinkedQueue<Map.Entry<String, Object>> pendingResponseFields = new ConcurrentLinkedQueue<>();

    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean dirtyQueued = new AtomicBoolean(false);
    private volatile long lastWakeupNanos;
    private volatile boolean processingSubmit;

    // :: Constructor

    public RemoteApplicationSupport(String id, Host host, RemoteAppSecurity security) {
        this.id = id;
        this.host = host;
        this.security = security;
    }

    /**
     * Must be called after the host's CubeApplication constructor has completed
     * (i.e., after presenterMap is initialized).
     */
    public void postConstruct(RemoteApplication owner) {
        this.expireMoment = System.currentTimeMillis() + timeSpan.toMillis();
        this.dataSecurity = new RemoteDataSecurity(security);
        this.browserPresenter = new RemoteBrowserPresenter(owner);
        this.putView(this.browserPresenter.getView());
        this.browserPresenter.update();
    }

    // :: Identity

    public String getId() {
        return this.id;
    }

    // :: Response envelope

    /**
     * Enqueues a field to be included in the next WebSocket response JSON.
     * Used for out-of-band data like access tokens.
     */
    public void addResponseField(String key, Object value) {
        this.pendingResponseFields.add(Map.entry(key, value));
    }

    /**
     * Emits an access token to the frontend for persistent storage.
     * The token is ciphered for transport; an empty/null token signals
     * the frontend to delete the stored token.
     */
    public void emitAccessToken(String token) {
        var value = (token != null && !token.isEmpty())
                ? this.dataSecurity.b64Cipher(token)
                : "";
        this.addResponseField("accessToken", value);
    }

    // :: Lifecycle

    public void setTimeSpan(Duration timeSpan) {
        this.timeSpan = timeSpan != null ? timeSpan : DEFAULT_TIME_SPAN;
    }

    public void extendLife() {
        this.expireMoment = System.currentTimeMillis() + timeSpan.toMillis();
    }

    public boolean isExpired(long now) {
        return this.expireMoment < now;
    }

    public void release() {
        this.browserPresenter.release();
    }

    // :: Instance ID generation

    public int nextInstanceId() {
        return this.instanceIdGen++;
    }

    // :: WebSocket session

    public WsContext getWsSession() {
        return wsSession;
    }

    public void setWsSession(WsContext wsSession) {
        this.wsSession = wsSession;
    }

    // :: Data security

    public RemoteDataSecurity getDataSecurity() {
        return dataSecurity;
    }

    // :: Browser presenter

    public RemoteBrowserPresenter getBrowserPresenter() {
        return this.browserPresenter;
    }

    // :: View management

    public RemoteViewImpl getViewInstanceById(String vsid) {
        return this.viewMap.get(vsid);
    }

    public Map<String, RemoteViewImpl> getViewMap() {
        return viewMap;
    }

    public void putView(RemoteViewImpl view) {
        this.viewMap.put(view.instanceId(), view);
    }

    public void removeView(String stateId) {
        this.dirtyViewMap.remove(stateId);
        var removed = this.viewMap.remove(stateId);
        if (removed != null) {
            this.releasedViews.add(stateId);
        }
    }

    public void markDirty(RemoteViewImpl view) {
        this.dirtyViewMap.put(view.instanceId(), view);
        host.getRegistry().enqueueDirty(this.getOwnerAsRemoteApp());
        if (this.processingSubmit) {
            host.getRegistry().triggerImmediateFlush(this.getOwnerAsRemoteApp());
        }
    }

    public void updateAllViews() {
        this.viewMap.forEach((_ignored, v) -> this.markDirty(v));
    }

    public void resetForReconnect() {
        this.lastSentFragment = null;
        this.historyDirty = true;
        this.releasedViews.clear();
        this.lastActiveViewsSentAt = System.currentTimeMillis();
        this.updateAllViews();
    }

    // :: History

    public void updateHistory() {
        this.historyDirty = true;
    }

    public void doUpdateHistory() {
        if (this.historyDirty) {
            var cubeApp = host.getCubeApp();
            var lastPlace = cubeApp.getLastPlace();
            var intent = new CubeIntent();
            intent.setPlace(lastPlace != null ? lastPlace : cubeApp.getRootPlace());
            cubeApp.publishParameters(intent);

            var b62 = Base62.BEAN;
            var signature = b62.encodeToString(security.signAsHash(intent.toString().getBytes(StandardCharsets.UTF_8)));
            intent.setParameter("sign", signature);

            cubeApp.setFragment(intent.toString());
            this.historyDirty = false;
            this.navigationAttempted = true;
        }
    }

    // :: Navigation

    public void safeGo(String path) {
        var intent = CubeIntent.parse(path);
        if (intent.getPlace() == null) {
            intent.setPlace(host.getCubeApp().getRootPlace());
        }

        var b62 = Base62.BEAN;

        var actualSignature = String.valueOf(intent.removeParameter("sign"));
        var expectedSignature = b62
                .encodeToString(security.signAsHash(intent.toString().getBytes(StandardCharsets.UTF_8)));

        if (!Objects.equals(actualSignature, expectedSignature)) {
            this.updateHistory();
            var cubeApp = host.getCubeApp();
            intent = cubeApp.newIntent();
            if (intent.getPlace() == null) {
                intent.setPlace(cubeApp.getRootPlace());
            }
        }

        this.lock.lock();
        try {
            host.performGo(intent);
        } finally {
            this.lock.unlock();
        }
    }

    // :: Error handling

    public void alertUnexpectedError(Log logger, String message, Throwable e) {
        this.lock.lock();
        try {
            this.browserPresenter.alertUnexpectedError(logger, message, e);
        } finally {
            this.lock.unlock();
        }
    }

    // :: Request processing

    public void sendResponse(Map<String, Object> request) throws Exception {
        var isPing = CoerceUtils.asBoolean(request.get("ping"), false).booleanValue();
        if (isPing) {
            this.extendLife();
            var signature = CoerceUtils.asString(request.get("secret"));
            if (StringUtils.isNotBlank(signature)) {
                this.dataSecurity.updateSecret(signature);
            }
            this.sendPingResponseIfNeeded();
            return;
        }

        var incomingRequestId = CoerceUtils.asLong(request.get("requestId"), null);
        if (incomingRequestId == null) {
            LOG.debug("Discarding request: incomingRequestId is null");
            return;
        }

        if (incomingRequestId <= this.lastRequestId) {
            LOG.debug("Discarding request: incoming={} <= last={}", incomingRequestId, this.lastRequestId);
            return;
        }

        try {
            this.processingSubmit = true;
            this.lastRequestId = incomingRequestId;
            this.runDispatchPhase(request);
            this.writeAndSendResponse(incomingRequestId, this.drainDirtyViews());
        } catch (Exception e) {
            var exn = new java.io.IOException("Sending response");
            exn.addSuppressed(e);
            throw exn;
        } finally {
            this.processingSubmit = false;
        }
    }

    // :: Flush (called by registry)

    public void flushDirtyViewsFromRegistry() {
        this.dirtyQueued.set(false);

        if (this.dirtyViewMap.isEmpty() || this.wsSession == null) {
            return;
        }

        var dirtyViews = drainDirtyViews();
        if (dirtyViews.isEmpty()) {
            return;
        }

        try {
            this.writeAndSendResponse(null, dirtyViews);
        } catch (Exception e) {
            LOG.error("Error during background flush", e);
        }
        // Re-enqueue if views became dirty during flush
        if (!this.dirtyViewMap.isEmpty()) {
            host.getRegistry().enqueueDirty(this.getOwnerAsRemoteApp());
        }
    }

    // :: Registry coordination

    public AtomicBoolean dirtyQueued() {
        return this.dirtyQueued;
    }

    public long getLastWakeupNanos() {
        return this.lastWakeupNanos;
    }

    public void setLastWakeupNanos(long nanos) {
        this.lastWakeupNanos = nanos;
    }

    public boolean isProcessingSubmit() {
        return this.processingSubmit;
    }

    // :: Private methods

    private RemoteApplication getOwnerAsRemoteApp() {
        // The host's CubeApp implements RemoteApplication
        return (RemoteApplication) host.getCubeApp();
    }

    private void sendPingResponseIfNeeded() {
        var hasReleasedViews = !this.releasedViews.isEmpty();
        var now = System.currentTimeMillis();
        var shouldSendActiveViews = (now - this.lastActiveViewsSentAt) >= ACTIVE_VIEWS_INTERVAL;

        if (!hasReleasedViews && !shouldSendActiveViews) {
            return;
        }

        var json = new JsonStreamWriter();
        json.beginObject();
        {
            if (hasReleasedViews) {
                json.name("releasedViews");
                json.beginArray();
                String released;
                while ((released = this.releasedViews.poll()) != null) {
                    json.value(released);
                }
                json.endArray();
            }

            if (shouldSendActiveViews) {
                this.lastActiveViewsSentAt = now;
                json.name("activeViews");
                json.beginArray();
                for (var vsid : this.viewMap.keySet()) {
                    json.value(vsid);
                }
                json.endArray();
            }
        }
        json.endObject();

        this.sendTextToClient(json.result());
    }

    private List<RemoteViewImpl> drainDirtyViews() {
        var snapshot = new java.util.ArrayList<RemoteViewImpl>(this.dirtyViewMap.size());
        var it = this.dirtyViewMap.values().iterator();
        while (it.hasNext()) {
            snapshot.add(it.next());
            it.remove();
        }
        return snapshot;
    }

    private void sendTextToClient(String text) {
        if (this.wsSession == null) {
            throw new AssertionError("Missing WebSocket Session");
        }
        this.wsSession.send(text);
    }

    // :: Dispatch Phase

    private void runDispatchPhase(Map<String, Object> request) throws Exception {
        this.updateSecret(request);
        this.updateApplicationState(request);

        @SuppressWarnings("unchecked")
        var eventList = (List<String>) request.get("event");
        if (eventList == null || eventList.isEmpty()) {
            return;
        }

        var viewNotFound = new MutableBoolean(false);
        for (var eventEntry : this.computeEventMap(eventList).entrySet()) {
            submitEvent(request, viewNotFound, eventEntry, eventEntry.getKey());
        }

        if (viewNotFound.isTrue()) {
            this.updateAllViews();
        }
    }

    private void submitEvent(Map<String, Object> request, MutableBoolean viewNotFound,
            Entry<String, Integer> eventEntry, String rawEvent) throws Exception {
        // <view-id>:<instance-id>:<event-code>
        var pos = rawEvent.lastIndexOf(':');
        if (pos != -1) {
            var viewId = rawEvent.substring(0, pos);
            var view = this.viewMap.get(viewId);
            if (view != null) {
                try {
                    var eventCode = Integer.parseInt(rawEvent.substring(pos + 1));
                    var eventQtde = eventEntry.getValue();

                    @SuppressWarnings("unchecked")
                    var formData = (Map<String, Object>) request.get(viewId);
                    if (formData == null) {
                        formData = Collections.emptyMap();
                    }

                    view.submit(eventCode, eventQtde, formData);
                } catch (@SuppressWarnings("java:S1181") Throwable e) {
                    this.alertUnexpectedError(LOG, "submitting event", e);
                    view.update();
                }
            } else {
                viewNotFound.setTrue();
            }
        }
    }

    private void updateApplicationState(Map<String, Object> request) {
        this.lock.lock();
        try {
            for (final Map.Entry<String, Object> entry : request.entrySet()) {
                var view = this.viewMap.get(entry.getKey());
                if (view != null) {
                    @SuppressWarnings("unchecked")
                    var formData = (Map<String, Object>) entry.getValue();
                    view.syncClientToServer(formData);
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    private void updateSecret(Map<String, Object> request) {
        var signature = CoerceUtils.asString(request.get("secret"));
        if (StringUtils.isNotBlank(signature)) {
            this.dataSecurity.updateSecret(signature);
        }
    }

    private Map<String, Integer> computeEventMap(List<String> eventList) {
        var eventMap = HashMap.<String, Integer>newHashMap(eventList.size());
        eventList.forEach(eventId -> {
            final Integer qtde = eventMap.get(eventId);
            if (qtde == null) {
                eventMap.put(eventId, 1);
            } else {
                eventMap.put(eventId, qtde + 1);
            }
        });
        return eventMap;
    }

    // :: Response Phase

    private void writeAndSendResponse(Long requestId, List<RemoteViewImpl> dirtyViews) {
        if (requestId == null && dirtyViews.isEmpty() && !this.historyDirty) {
            return;
        }

        this.doUpdateHistory();

        var strWriter = new StringWriter();
        var json = new JsonExtensibleObjectOutput(new JsonWriter(strWriter));
        try {
            this.writeResponseJson(json, requestId, dirtyViews);
        } finally {
            json.flush();
        }

        var jsonResponse = strWriter.toString();
        if (jsonResponse.length() > 2) {
            this.sendTextToClient(jsonResponse);
        }
    }

    private void writeResponseJson(ExtensibleObjectOutput json, Long requestId, List<RemoteViewImpl> dirtyViews) {
        json.beginObject();
        {
            if (requestId != null) {
                json.name("requestId").value(requestId);
            }

            // Drain pending envelope fields
            Map.Entry<String, Object> field;
            while ((field = this.pendingResponseFields.poll()) != null) {
                json.name(field.getKey());
                if (field.getValue() == null) {
                    json.nullValue();
                } else if (field.getValue() instanceof String s) {
                    json.value(s);
                } else if (field.getValue() instanceof Number n) {
                    json.value(n);
                } else if (field.getValue() instanceof Boolean b) {
                    json.value(b);
                } else {
                    json.value(field.getValue().toString());
                }
            }

            var currentFragment = host.getCubeApp().getFragment();
            if (currentFragment != null && (this.navigationAttempted || !currentFragment.equals(this.lastSentFragment))) {
                this.lastSentFragment = currentFragment;
                this.navigationAttempted = false;
                json.name("uri").value(currentFragment);
            }

            if (!dirtyViews.isEmpty()) {
                json.name("states");
                json.beginArray();

                for (var view : dirtyViews) {
                    this.lock.lock();
                    try {
                        view.presenter().commitComputedState();
                    } finally {
                        this.lock.unlock();
                    }
                    view.writeState(json);
                }

                json.endArray();
            }
        }
        json.endObject();
    }
}
