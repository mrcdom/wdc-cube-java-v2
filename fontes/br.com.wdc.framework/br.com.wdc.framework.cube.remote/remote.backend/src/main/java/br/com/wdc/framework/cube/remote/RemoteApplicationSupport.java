package br.com.wdc.framework.cube.remote;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.gson.JsonExtensibleObjectOutput;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.commons.serialization.JsonStreamWriter;
import br.com.wdc.framework.cube.CubeApplication;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.Scope;
import io.javalin.websocket.WsContext;

/**
 * Reusable implementation of the {@link RemoteApplication} contract.
 * <p>
 * Concrete application classes (e.g., StoreApplicationImpl) delegate
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
    private String clientIp;

    private RemoteBrowserPresenter browserPresenter;
    private RemoteApplication owner;
    private final Map<String, RemoteViewImpl> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, RemoteViewImpl> viewMap = new ConcurrentHashMap<>();
    /** The view of each scope drawn for the client. */
    private final Map<Scope, RemoteViewImpl> scopeViewMap = Collections.synchronizedMap(new IdentityHashMap<>());
    private final ConcurrentLinkedQueue<String> releasedViews = new ConcurrentLinkedQueue<>();
    private long lastRequestId = -1;
    private long lastActiveViewsSentAt;
    private String lastSentFragment;
    private boolean historyDirty;
    private boolean navigationAttempted;
    private int instanceIdGen = 1;

    private final ReentrantLock lock = new ReentrantLock();

    /**
     * One message is written at a time: the answer to a request and the background flush both take the dirty views
     * and write them. A view born while a scope is being written goes in the message that refers to it, instead of
     * in the other writer's, which a client could read before the reference.
     */
    private final ReentrantLock writeLock = new ReentrantLock();
    private final AtomicBoolean dirtyQueued = new AtomicBoolean(false);
    private volatile long lastWakeupNanos;
    private volatile boolean processingSubmit;

    private final RemoteClientStorage clientStorage = new RemoteClientStorage();

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
        this.dataSecurity = new RemoteDataSecurity();
        this.clientStorage.init(this.dataSecurity);
        this.owner = owner;
        this.browserPresenter = new RemoteBrowserPresenter(owner);
        this.putView(this.browserPresenter.getView());
        this.browserPresenter.update();
    }

    // :: Identity

    public String getId() {
        return this.id;
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
        var attached = wsSession != null && this.wsSession != wsSession;
        this.wsSession = wsSession;
        this.clientIp = wsSession != null ? resolveClientIp(wsSession) : "127.0.0.1";
        // Views dirtied while there was no socket (the browser's, as the application is born) go out now
        if (attached && !this.dirtyViewMap.isEmpty()) {
            host.getRegistry().enqueueDirty(this.getOwnerAsRemoteApp());
        }
    }
    
    public String getClientIp() {
        return clientIp;
    }

    // :: Data security

    public RemoteDataSecurity getDataSecurity() {
        return dataSecurity;
    }

    // :: Client storage

    public RemoteClientStorage getClientStorage() {
        return clientStorage;
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

    /** The view that draws the scope for the client, made the first time a scope that holds it is written. */
    public RemoteViewImpl viewOf(Scope scope) {
        synchronized (this.scopeViewMap) {
            var view = this.scopeViewMap.get(scope);
            if (view == null) {
                view = RemoteViewImpl.forScope(this.getOwnerAsRemoteApp(), scope);
                this.scopeViewMap.put(scope, view);
            }
            return view;
        }
    }

    /** The view no longer draws its scope. */
    public void forgetView(RemoteViewImpl view) {
        this.scopeViewMap.remove(view.scope(), view);
    }

    public void putView(RemoteViewImpl view) {
        this.viewMap.put(view.instanceId(), view);
    }

    public void removeView(String instanceId) {
        this.dirtyViewMap.remove(instanceId);
        var removed = this.viewMap.remove(instanceId);
        if (removed != null) {
            this.releasedViews.add(instanceId);
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

            intent.setParameter("sign", security.sign(intent.toString()));

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

        var actualSignature = intent.removeParameter("sign");
        if (!security.verify(intent.toString(), actualSignature == null ? null : String.valueOf(actualSignature))) {
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

    /**
     * The host's alert for an unexpected error (alert {@code -1}, with the cause's message): the client words it in its
     * user's language.
     */
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
            this.drainAndSend(incomingRequestId);
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

        try {
            this.drainAndSend(null);
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

        this.writeLock.lock();
        try {
            this.sendTextToClient(json.result());
        } finally {
            this.writeLock.unlock();
        }
    }

    /** Takes the dirty views and writes them, with the request's id when it answers one; one writer at a time. */
    private void drainAndSend(Long requestId) {
        this.writeLock.lock();
        try {
            var dirtyViews = this.drainDirtyViews();
            if (requestId == null && dirtyViews.isEmpty()) {
                return;
            }
            this.writeAndSendResponse(requestId, dirtyViews);
        } finally {
            this.writeLock.unlock();
        }
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
        this.syncScopes(request);

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
                    // through the application, which may show it its own way
                    this.owner.alertUnexpectedError(LOG, "submitting event", e);
                    view.update();
                }
            } else {
                viewNotFound.setTrue();
            }
        }
    }

    private void syncScopes(Map<String, Object> request) {
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

            // Client storage deltas
            var storageDelta = this.clientStorage.drainDeltas();
            if (storageDelta != null) {
                json.name("storage");
                json.beginObject();
                for (var scopeEntry : storageDelta.entrySet()) {
                    json.name(scopeEntry.getKey());
                    json.beginObject();
                    for (var kv : scopeEntry.getValue().entrySet()) {
                        json.name(kv.getKey());
                        if (kv.getValue() == null) {
                            json.nullValue();
                        } else {
                            json.value(kv.getValue());
                        }
                    }
                    json.endObject();
                }
                json.endObject();
            }

            var currentFragment = host.getCubeApp().getFragment();
            if (currentFragment != null && (this.navigationAttempted || !currentFragment.equals(this.lastSentFragment))) {
                this.lastSentFragment = currentFragment;
                this.navigationAttempted = false;
                json.name("uri").value(currentFragment);
            }

            if (!dirtyViews.isEmpty()) {
                json.name("scopes");
                json.beginArray();

                // The presenters' computed state, once, before any scope is written
                this.lock.lock();
                try {
                    var cubeApp = host.getCubeApp();
                    if (cubeApp != null) {
                        cubeApp.commitComputedState();
                    }
                    this.browserPresenter.commitComputedState();
                } finally {
                    this.lock.unlock();
                }

                var queue = new java.util.ArrayDeque<RemoteViewImpl>(dirtyViews);
                var written = new java.util.HashSet<String>();
                while (!queue.isEmpty()) {
                    var view = queue.poll();
                    if (view.isReleased() || !written.add(view.instanceId())) {
                        continue;
                    }
                    view.writeScope(json);

                    // Views born while writing (a segment or a row drawn for the first time) go in this same
                    // response: the scope just written already refers to them
                    if (queue.isEmpty()) {
                        for (var born : this.dirtyViewMap.values()) {
                            if (!written.contains(born.instanceId())) {
                                this.dirtyViewMap.remove(born.instanceId());
                                queue.add(born);
                            }
                        }
                    }
                }

                json.endArray();
            }
        }
        json.endObject();
    }
    
    private static String resolveClientIp(WsContext ctx) {
        var fwd = ctx.header("X-Forwarded-For");
        if (StringUtils.isNotBlank(fwd)) {
            // X-Forwarded-For may come as "client, proxy1, proxy2" — the first one is kept.
            int comma = fwd.indexOf(',');
            return comma > 0 ? fwd.substring(0, comma).trim() : fwd.trim();
        }
        try {
            // Jetty 12 (websocket-jetty-api 12.x): getRemoteSocketAddress(), not getRemoteAddress().
            if (ctx.session.getRemoteSocketAddress() instanceof java.net.InetSocketAddress isa
                    && isa.getAddress() != null) {
                return isa.getAddress().getHostAddress();
            }
        } catch (Exception e) {
            LOG.debug("Could not resolve the WebSocket remote IP", e);
        }
        return null;
    }
}
