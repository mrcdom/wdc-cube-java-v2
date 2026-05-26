package br.com.wdc.shopping.view.remote.skeleton.viewimpl;

import java.io.IOException;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.gson.JsonExtensibleObjectOutput;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.remote.skeleton.spi.WebSocketConnection;
import br.com.wdc.shopping.view.remote.skeleton.util.AppSecurity;
import br.com.wdc.shopping.view.remote.skeleton.util.DataSecurity;
import br.com.wdc.shopping.view.remote.skeleton.util.GenericViewImpl;

public class ApplicationReactImpl extends ShoppingApplication {

    private static final Log LOG = Log.getLogger(ApplicationReactImpl.class);

    public static final Duration DEFAULT_TIME_SPAN = Duration.ofMinutes(3);

    static {
        RootPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        LoginPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        HomePresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        ProductPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        CartPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        ReceiptPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        ProductsPanelPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
        PurchasesPanelPresenter.createView = p -> new GenericViewImpl(p.app, p, p.state, p.skeleton());
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    public ApplicationReactImpl(String id) {
        this.id = id;
        this.removeInstanceAction = ThrowingRunnable.noop();
        this.postConstruct();
    }

    @Override
    protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
        return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
    }

    // :: Instance

    private final String id;
    private long expireMoment;

    private DataSecurity dataSecurity;
    private transient @SuppressWarnings("java:S2065") WebSocketConnection wsSession;
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    private ThrowingRunnable removeInstanceAction;

    private RootPresenter rootPresenter;
    private final Map<String, GenericViewImpl> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, GenericViewImpl> viewMap = new ConcurrentHashMap<>();
    private long lastRequestId;
    private boolean historyDirty;
    private BrowserPresenter browserPresenter;
    private int instanceIdGen = 1;

    private final ReentrantLock lock = new ReentrantLock();
    final AtomicBoolean dirtyQueued = new AtomicBoolean(false);
    volatile long lastWakeupNanos;

    protected void postConstruct() {
        this.expireMoment = System.currentTimeMillis() + DEFAULT_TIME_SPAN.toMillis();
        this.dataSecurity = new DataSecurity();
        this.browserPresenter = new BrowserPresenter(this);

        // View que representa o navegador
        this.putView(this.browserPresenter.getView());
        this.browserPresenter.update();
    }

    @Override
    public void release() {
        try {
            try {
                this.browserPresenter.release();
                this.removeInstanceAction.runThrows();
                this.removeInstanceAction = ThrowingRunnable.noop();
                super.release();
            } finally {
                ApplicationReactRegistry.remove(id);
                LOG.info("Application removed: {}", this.id);
            }
        } catch (Exception caught) {
            LOG.error("Running removeInstanceAction", caught);
        }
    }

    public void extendLife() {
        this.expireMoment = System.currentTimeMillis() + DEFAULT_TIME_SPAN.toMillis();
    }

    public String getId() {
        return this.id;
    }

    boolean isExpired(long now) {
        return this.expireMoment < now;
    }

    public int nextInstanceId() {
        return this.instanceIdGen++;
    }

    public void addReleaseAction(ThrowingRunnable newAction) {
        var oldAction = this.removeInstanceAction;
        this.removeInstanceAction = () -> {
            newAction.runThrows();
            oldAction.runThrows();
        };
    }

    public boolean isAuthenticated() {
        return this.getSubject() != null;
    }

    public DataSecurity getDataSecurity() {
        return dataSecurity;
    }

    public WebSocketConnection getWsSession() {
        return wsSession;
    }

    public void setWsSession(WebSocketConnection wsSession) {
        this.wsSession = wsSession;
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }

    @Override
    public RootPresenter getRootPresenter() {
        return this.rootPresenter != null ? this.rootPresenter : super.getRootPresenter();
    }

    public BrowserPresenter getBrowserPresenter() {
        return this.browserPresenter;
    }

    public void setRootPresenter(RootPresenter presenter) {
        this.rootPresenter = presenter;
    }

    public GenericViewImpl getViewInstanceById(String vsid) {
        return this.viewMap.get(vsid);
    }

    public Map<String, GenericViewImpl> getViewMap() {
        return viewMap;
    }

    public String b64Cipher(String text) {
        return this.dataSecurity.b64Cipher(text);
    }

    public String b64Decipher(String b64Text) {
        return this.dataSecurity.b64Decipher(b64Text);
    }

    @Override
    public void updateHistory() {
        this.historyDirty = true;
    }

    public void doUpdateHistory() {
        if (this.historyDirty) {
            var security = AppSecurity.BEAN;
            var intent = new CubeIntent();
            intent.setPlace(lastPlace != null ? lastPlace : this.getRootPlace());
            this.publishParameters(intent);

            var b62 = Base62.BEAN;
            var signature = b62.encodeToString(security.signAsHash(intent.toString().getBytes(StandardCharsets.UTF_8)));
            intent.setParameter("sign", signature);

            this.fragment = intent.toString();
            this.historyDirty = false;
        }
    }

    public void safeGo(String path) throws Exception {
        var security = AppSecurity.BEAN;
        var intent = CubeIntent.parse(path);
        if (intent.getPlace() == null) {
            intent.setPlace(this.getRootPlace());
        }

        var b62 = Base62.BEAN;

        var actualSignature = String.valueOf(intent.removeParameter("sign"));
        var expectedSignature = b62
                .encodeToString(security.signAsHash(intent.toString().getBytes(StandardCharsets.UTF_8)));

        if (!Objects.equals(actualSignature, expectedSignature)) {
            this.updateHistory();
            intent = this.newIntent();
            if (intent.getPlace() == null) {
                intent.setPlace(this.getRootPlace());
            }
        }

        this.lock.lock();
        try {
            this.go(intent);
        } finally {
            this.lock.unlock();
        }
    }

    public void putView(GenericViewImpl view) {
        this.viewMap.put(view.instanceId(), view);
    }

    public GenericViewImpl removeView(String stateId) {
        this.dirtyViewMap.remove(stateId);
        return this.viewMap.remove(stateId);
    }

    public void markDirty(GenericViewImpl view) {
        this.dirtyViewMap.put(view.instanceId(), view);
        ApplicationReactRegistry.enqueueDirty(this);
    }

    public void updateAllViews() {
        this.viewMap.forEach((_ignored, v) -> this.markDirty(v));
    }

    @Override
    public void alertUnexpectedError(Log logger, String message, Throwable e) {
        this.lock.lock();
        try {
            this.browserPresenter.alertUnexpectedError(message, e);
        } finally {
            this.lock.unlock();
        }
    }

    public void sendResponse(Map<String, Object> request) throws Exception {
        // Discard stale requests (retransmissions after reconnect)
        var incomingRequestId = CoerceUtils.asLong(request.get("requestId"), null);
        var isPing = CoerceUtils.asBoolean(request.get("ping"), false).booleanValue();
        if (!isPing && incomingRequestId != null && incomingRequestId <= this.lastRequestId) {
            LOG.debug("Discarding stale request: incoming={} last={}", incomingRequestId, this.lastRequestId);
            return;
        }

        try {
            new DispatchPhaseBhv(this).run(request);
            new ResponsePhaseBhv(this).run(request);
        } catch (Exception e) {
            IOException exn = new IOException("Sending response");
            exn.addSuppressed(e);
            throw exn;
        } finally {
            try {
                if (!this.dirtyViewMap.isEmpty()) {
                    ApplicationReactRegistry.enqueueDirty(this);
                    ApplicationReactRegistry.triggerImmediateFlush(this);
                }
            } catch (Exception e) {
                LOG.error("error in endRequest", e);
            }
        }
    }

    // :: Internal

    void flushDirtyViewsFromRegistry() {
        this.dirtyQueued.set(false);

        if (this.dirtyViewMap.isEmpty() || this.wsSession == null) {
            return;
        }

        try {
            var pushRequest = Map.<String, Object>of();
            new ResponsePhaseBhv(this).run(pushRequest);
        } catch (Exception e) {
            LOG.error("Error during background flush", e);
        }
        // Re-enqueue if views became dirty during flush
        if (!this.dirtyViewMap.isEmpty()) {
            ApplicationReactRegistry.enqueueDirty(this);
        }
    }

    protected void sendTextToClient(String text) {
        if (this.wsSession == null) {
            throw new AssertionError("Missing WebSocket Session");
        }
        this.wsSession.sendText(text);
    }

    /**
     * Processes client request data and updates application state.
     * 
     * <p>
     * Concurrency is managed via a per-instance {@link ReentrantLock}, which avoids pinning virtual threads to carrier
     * threads (unlike {@code synchronized}). The lock protects view map mutations and dirty state.
     * </p>
     * 
     * @param request the client request data containing form data and events
     * @return true if processing succeeded
     * @throws Exception if request processing fails
     */
    private static class DispatchPhaseBhv {

        private ApplicationReactImpl me;

        public DispatchPhaseBhv(ApplicationReactImpl me) {
            this.me = me;
        }

        public boolean run(Map<String, Object> request) throws Exception {
            this.updateSecret(request);
            this.updateApplicationState(request);

            // Nothing to do if no event has bean sent
            @SuppressWarnings("unchecked")
            var eventList = (List<String>) request.get("event");
            if (eventList == null || eventList.isEmpty()) {
                return false;
            }

            var viewNotFound = new MutableBoolean(false);
            for (var eventEntry : this.computeEventMap(eventList).entrySet()) {
                submitEvent(request, viewNotFound, eventEntry, eventEntry.getKey());
            }

            if (viewNotFound.isTrue()) {
                me.updateAllViews();
            }

            return true;
        }

        private void submitEvent(Map<String, Object> request, MutableBoolean viewNotFound,
                Entry<String, Integer> eventEntry, String rawEvent)
                throws Exception {
            // <view-id>:<instance-id>:<event-code>
            var pos = rawEvent.lastIndexOf(':');
            if (pos != -1) {
                var viewId = rawEvent.substring(0, pos);
                var view = me.viewMap.get(viewId);
                if (view != null) {
                    try {
                        var eventCode = Integer.parseInt(rawEvent.substring(pos + 1));
                        var eventQtde = eventEntry.getValue();

                        @SuppressWarnings("unchecked")
                        var formData = (Map<String, Object>) request.get(viewId);
                        if (formData == null) {
                            formData = Collections.emptyMap();
                        }

                        me.lock.lock();
                        try {
                            view.submit(eventCode, eventQtde, formData);
                        } finally {
                            me.lock.unlock();
                        }
                    } catch (@SuppressWarnings("java:S1181") Throwable e) {
                        me.alertUnexpectedError(LOG, e.getMessage(), e);
                        view.update();
                    }
                } else {
                    viewNotFound.setTrue();
                }
            }
        }

        private void updateApplicationState(Map<String, Object> request) {
            me.lock.lock();
            try {
                // First, update application state
                for (final Map.Entry<String, Object> entry : request.entrySet()) {
                    var view = me.viewMap.get(entry.getKey());
                    if (view != null) {
                        @SuppressWarnings("unchecked")
                        var formData = (Map<String, Object>) entry.getValue();
                        view.syncClientToServer(formData);
                    }
                }
            } finally {
                me.lock.unlock();
            }
        }

        private void updateSecret(Map<String, Object> request) {
            var signature = CoerceUtils.asString(request.get("secret"));
            if (StringUtils.isNotBlank(signature)) {
                me.getDataSecurity().updateSecret(signature);
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

    }

    /**
     * Generates response data from dirty views and sends it back to the client.
     * 
     * <p>
     * Concurrency is managed via a per-instance {@link ReentrantLock} to protect dirty view state without pinning
     * virtual threads. The lock is not held during WebSocket message transmission.
     * </p>
     * 
     * @param request   the incoming client request (used to extract requestId)
     * @param fromFlush true if this is a flush request (empty request)
     * @return true if response data was sent, false if no changes
     * @throws Exception if response generation or transmission fails
     */
    private static class ResponsePhaseBhv {

        private ApplicationReactImpl me;

        public ResponsePhaseBhv(ApplicationReactImpl me) {
            this.me = me;
        }

        public boolean run(Map<String, Object> request) {
            var requestId = getRequestId(request);
            if (requestId != null) {
                me.lastRequestId = requestId;
            }
            var isPing = isPing(request);

            if (!isPing && hasNoDirtyViews()) {
                return false;
            }

            me.doUpdateHistory();

            // Write response
            var strWriter = new StringWriter();
            var json = new JsonExtensibleObjectOutput(new JsonWriter(strWriter));
            try {
                this.writeResponse(json, requestId, isPing);
            } finally {
                json.flush();
            }

            // Send response to the client
            var jsonResponse = strWriter.toString();
            me.sendTextToClient(jsonResponse);

            return true;
        }

        private Long getRequestId(Map<String, Object> request) {
            return CoerceUtils.asLong(request.get("requestId"), null);
        }

        private boolean isPing(Map<String, Object> request) {
            return CoerceUtils.asBoolean(request.get("ping"), false);
        }

        private boolean hasNoDirtyViews() {
            return !me.historyDirty && me.dirtyViewMap.isEmpty();
        }

        private void writeResponse(ExtensibleObjectOutput json, Long requestId, boolean isPing) {
            json.beginObject();
            {
                if (requestId != null) {
                    json.name("requestId").value(requestId);
                }

                if (isPing) {
                    json.name("ping").value(true);
                }

                if (me.getFragment() != null) {
                    json.name("uri").value(me.getFragment());
                }

                if (me.dirtyViewMap.size() > 0) {
                    json.name("states");
                    json.beginArray();

                    var dirtyViews = me.dirtyViewMap.values().iterator();

                    while (dirtyViews.hasNext()) {
                        var view = dirtyViews.next();
                        me.lock.lock();
                        try {
                            view.presenter().commitComputedState();
                            view.writeState(json);
                        } finally {
                            me.lock.unlock();
                        }
                        dirtyViews.remove();
                    }

                    json.endArray();
                }
            }
            json.endObject();
        }

    }

}
