package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonWriter;

import br.com.wdc.framework.commons.codec.Base62;
import br.com.wdc.framework.commons.concurrent.ScheduledExecutor;
import br.com.wdc.framework.commons.function.Registration;
import br.com.wdc.framework.commons.function.ThrowingRunnable;
import br.com.wdc.framework.commons.gson.JsonExtensibleObjectOutput;
import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.framework.cube.CubeIntent;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.react.skeleton.spi.WebSocketConnection;
import br.com.wdc.shopping.view.react.skeleton.util.AppSecurity;
import br.com.wdc.shopping.view.react.skeleton.util.DataSecurity;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class ApplicationReactImpl extends ShoppingApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ApplicationReactImpl.class);

    public static final Duration DEFAULT_TIME_SPAN = Duration.ofMinutes(3);

    private static final Duration PUSH_DELAY = Duration.ofMillis(200);

    static {
        RootPresenter.createView = RootReactViewImpl::new;
        LoginPresenter.createView = LoginReactViewImpl::new;
        HomePresenter.createView = HomeReactViewImpl::new;
        ProductPresenter.createView = ProductReactViewImpl::new;
        CartPresenter.createView = CartReactViewImpl::new;
        ReceiptPresenter.createView = ReceiptReactViewImpl::new;
        ProductsPanelPresenter.createView = ProductsPanelReactViewImpl::new;
        PurchasesPanelPresenter.createView = PurchasesPanelReactViewImpl::new;
    }

    private static ConcurrentHashMap<String, ApplicationReactImpl> instanceMap = new ConcurrentHashMap<>();

    public static ApplicationReactImpl get(String appId) {
        if (StringUtils.isBlank(appId)) {
            return null;
        }
        return instanceMap.get(appId);
    }

    public static ApplicationReactImpl getOrCreate(String appId, String path) {
        var request = new HashMap<String, Object>();

        {
            var browserViewState = new HashMap<String, Object>();
            browserViewState.put("p.path", path);

            request.put(BrowserReactViewImpl.VSID, browserViewState);
        }

        return ApplicationReactImpl.getOrCreate(appId, request);
    }

    public static ApplicationReactImpl getOrCreate(String appId, Map<String, Object> request) {
        return instanceMap.computeIfAbsent(appId, _ -> ApplicationReactImpl.createApp(appId, request));
    }

    private static ApplicationReactImpl createApp(String appId, Map<String, Object> request) {
        var app = new ApplicationReactImpl(appId);
        try {
            app.addReleaseAction(() -> instanceMap.remove(appId));

            String path = app.getFragment();
            {
                @SuppressWarnings("unchecked")
                Map<String, Object> browserViewState = (Map<String, Object>) request
                        .get(BrowserReactViewImpl.VSID);
                if (browserViewState != null) {
                    path = (String) browserViewState.get("p.path");
                    if (StringUtils.isBlank(path)) {
                        path = app.getFragment();
                    }
                }
            }

            app.safeGo(path);
        } catch (Exception caught) {
            app.release();
            return ExceptionUtils.rethrow(caught);
        }
        return app;
    }

    public static ApplicationReactImpl remove(String sessionId) {
        return instanceMap.remove(sessionId);
    }

    public static void removeExpireds() {
        var now = System.currentTimeMillis();

        var appItertor = instanceMap.values().iterator();
        while (appItertor.hasNext()) {
            var app = appItertor.next();
            if (app.expireMoment < now) {
                if (app.wsSession == null) {
                    app.release();
                } else {
                    app.extendLife();
                }
            }
        }
    }

    public ApplicationReactImpl(String id) {
        this.id = id;
        this.removeInstanceAction = ThrowingRunnable.noop();
        this.postConstruct();
    }

    // :: Instance

    private final String id;
    private long expireMoment;

    private DataSecurity dataSecurity;
    private transient @SuppressWarnings("java:S2065") WebSocketConnection wsSession;
    private ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

    private ThrowingRunnable removeInstanceAction;

    private RootPresenter rootPresenter;
    private final Map<String, GenericViewImpl> dirtyViewMap = new LinkedHashMap<>();
    private final Map<String, GenericViewImpl> viewMap = new HashMap<>();
    private long lastRequestId;
    private boolean historyDirty;
    private BrowserReactViewImpl browserView;
    private int instanceIdGen = 1;

    private transient volatile boolean requestInProgress;
    private transient volatile Registration pushRegistration = Registration.noop();

    protected void postConstruct() {
        this.dataSecurity = new DataSecurity();
        this.browserView = new BrowserReactViewImpl(this);
        this.viewMap.put(browserView.instanceId(), browserView);
        this.dirtyViewMap.put(browserView.instanceId(), browserView);
        this.expireMoment = System.currentTimeMillis() + DEFAULT_TIME_SPAN.toMillis();
    }

    @Override
    public void release() {
        try {
            try {
                this.pushRegistration.remove();
                this.pushRegistration = Registration.noop();
                this.browserView.release();
                this.removeInstanceAction.runThrows();
                this.removeInstanceAction = ThrowingRunnable.noop();
                super.release();
            } finally {
                ApplicationReactImpl.instanceMap.remove(id);
                LOG.info("Application removed: {}", this.id);
            }
        } catch (Exception caught) {
            LOG.error("Running removeInstanceAction", caught);
        }
    }

    public void extendLife() {
        this.expireMoment = System.currentTimeMillis() + DEFAULT_TIME_SPAN.toMillis();
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
        return this.rootPresenter;
    }

    public void setRootPresenter(RootPresenter presenter) {
        this.rootPresenter = presenter;
    }

    public GenericViewImpl getViewInstanceById(String vsid) {
        synchronized (this) {
            return this.viewMap.get(vsid);
        }
    }

    public Map<String, GenericViewImpl> getViewMap() {
        return viewMap;
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
        this.go(intent);
    }

    public void putView(GenericViewImpl view) {
        this.viewMap.put(view.instanceId(), view);
    }

    public GenericViewImpl removeView(String stateId) {
        this.dirtyViewMap.remove(stateId);
        return this.viewMap.remove(stateId);
    }

    public synchronized void markDirty(GenericViewImpl view) {
        this.dirtyViewMap.put(view.instanceId(), view);
        if (!this.requestInProgress && this.wsSession != null) {
            this.schedulePush();
        }
    }

    public void updateAllViews() {
        this.viewMap.forEach((_, v) -> this.markDirty(v));
    }

    @Override
    public void alertUnexpectedError(Logger logger, String message, Throwable e) {
        this.browserView.alertUnexpectedError(message, e);
    }

    public synchronized void beginRequest() {
        this.requestInProgress = true;
        this.pushRegistration.remove();
        this.pushRegistration = Registration.noop();
    }

    public synchronized void endRequest() {
        this.requestInProgress = false;
    }

    public void sendResponse(Map<String, Object> request) throws Exception {
        try {
            this.beginRequest();
            new DispatchPhaseBhv(this).run(request);
            new ResponsePhaseBhv(this).run(request);
        } catch (Exception e) {
            IOException exn = new IOException("Sending response");
            exn.addSuppressed(e);
            throw exn;
        } finally {
            try {
                this.endRequest();
            } catch (Exception e) {
                LOG.error("error in endRequest", e);
            }
        }
    }

    // :: Internal

    private void schedulePush() {
        if (this.pushRegistration != Registration.noop()) {
            return;
        }
        var scheduler = ScheduledExecutor.BEAN.get();
        if (scheduler == null) {
            return;
        }
        this.pushRegistration = scheduler.schedule(this::executePush, PUSH_DELAY);
    }

    private void executePush() {
        synchronized (this) {
            this.pushRegistration = Registration.noop();
            if (this.requestInProgress || this.dirtyViewMap.isEmpty() || this.wsSession == null) {
                return;
            }
        }
        try {
            var pushRequest = Map.<String, Object>of("requestId", this.lastRequestId);
            new ResponsePhaseBhv(this).run(pushRequest);
        } catch (Exception e) {
            LOG.error("Error during server push", e);
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
     * This method is synchronized to ensure atomic updates to the view map when multiple concurrent WebSocket
     * connections process requests for the same application instance. Synchronization is safe and efficient with
     * Virtual Threads because Virtual Threads yield when waiting for monitor locks without blocking the underlying OS
     * thread, allowing other Virtual Threads to make progress.
     * </p>
     * 
     * <p>
     * The critical section is kept minimal (milliseconds) and primarily involves HashMap lookups and view state
     * updates, making contention unlikely in practice.
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
                Entry<String, Integer> eventEntry, String rawEvent) throws Exception {
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

                        view.submit(eventCode, eventQtde, formData);
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
            // First, update application state
            for (final Map.Entry<String, Object> entry : request.entrySet()) {
                var view = me.viewMap.get(entry.getKey());
                if (view != null) {
                    @SuppressWarnings("unchecked")
                    var formData = (Map<String, Object>) entry.getValue();
                    view.syncClientToServer(formData);
                }
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
     * This method is synchronized to protect concurrent access to view state. With Virtual Threads, multiple WebSocket
     * handlers can safely call this method simultaneously on the same application instance without blocking OS threads.
     * </p>
     * 
     * <p>
     * Synchronization scope: view dirty state tracking and response generation. Does not hold the lock during WebSocket
     * message transmission.
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
            var requestId = me.lastRequestId = getRequestId(request);
            var isPing = isPing(request);

            if (!isPing && hasNoDirtyViews()) {
                return false;
            }

            this.commitComputedState();

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
            return CoerceUtils.asLong(request.get("requestId"), me.lastRequestId);
        }

        private boolean isPing(Map<String, Object> request) {
            return CoerceUtils.asBoolean(request.get("ping"), false);
        }

        private boolean hasNoDirtyViews() {
            return !me.historyDirty && me.dirtyViewMap.isEmpty();
        }

        private void commitComputedState() {
            me.presenterMap.forEach((_, presenter) -> {
                try {
                    presenter.commitComputedState();
                } catch (Exception cause) {
                    LOG.error("presenter.commitComputedState()", cause);
                }
            });
        }

        private void writeResponse(ExtensibleObjectOutput json, Long requestId, boolean isPing) {
            json.beginObject();
            {
                json.name("requestId").value(requestId);

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
                        view.writeState(json);
                        dirtyViews.remove();
                    }

                    json.endArray();
                }
            }
            json.endObject();
        }

    }

}
