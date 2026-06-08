package br.com.wdc.framework.cube.remote.bridge.java;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.framework.cube.remote.bridge.java.model.HostResponse;
import br.com.wdc.framework.cube.remote.bridge.java.model.SecretContext;
import br.com.wdc.framework.cube.remote.bridge.java.model.ViewStateMap;
import br.com.wdc.framework.cube.remote.bridge.java.model.ViewStateSnapshot;

/**
 * Main facade for connecting a Java program to the Host as a Shell.
 * <p>
 * Usage:
 * <pre>{@code
 * var client = HostClient.connect("http://localhost:8080");
 * HostResponse initial = client.awaitResponse();          // async initial state push
 * client.navigate(initial.uri());                         // navigate via signed URI
 * HostResponse afterNav = client.awaitResponse();
 * client.submit("productList:1", 3, Map.of());            // submit view event
 * HostResponse after = client.awaitResponse();
 * ViewStateSnapshot cart = client.viewState("cart:1");
 * client.close();
 * }</pre>
 *
 * <h3>Protocol flow</h3>
 * <ol>
 *   <li>{@code GET /api/session/init} -> {@code {appId, appSKey}}</li>
 *   <li>Build secret from {@code appSKey} (RSA + PBKDF2 key derivation)</li>
 *   <li>WS connect {@code ws://host/dispatcher/{appId}} (sub-protocol {@code wdc})</li>
 *   <li>Send: {@code {"secret": "...", "event": []}} -- no requestId</li>
 *   <li>Server responds asynchronously with initial states push</li>
 *   <li>Navigate: {@code {"requestId": N, "event": ["7b32e816a191:0:-1"], "7b32e816a191:0": {"p.path": uri}}}</li>
 *   <li>Submit: {@code {"requestId": N, "event": ["instanceId:eventCode"], "instanceId": {formData}}}</li>
 * </ol>
 */
public final class HostClient implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HostClient.class);

    /**
     * The instanceId of the browser ViewState — the root of the server-side view composition tree.
     * Use {@code viewState(BROWSER_VSID)} to read the browser state directly.
     */
    public static final String BROWSER_VSID = HostClientSession.BROWSER_VSID;

    @SuppressWarnings("java:S1075") // Protocol-defined path, not deployment-specific
    private static final String SESSION_INIT_PATH = "/api/session/init";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final Gson GSON = new GsonBuilder()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private static final TypeToken<Map<String, Object>> MAP_TYPE = new TypeToken<>() {};

    private final String serverUrl;
    private final SecretContext secret;
    private final HostClientSession session;
    private final ViewStateMap states;
    private final AtomicLong requestCounter = new AtomicLong(0);

    private HostClient(String serverUrl, SecretContext secret, HostClientSession session, ViewStateMap states) {
        this.serverUrl = serverUrl;
        this.secret = secret;
        this.session = session;
        this.states = states;
    }

    // :: Factory

    /**
     * Connects to the Host at the given base URL (e.g. {@code http://localhost:8080}).
     * <p>
     * This method:
     * <ol>
     *   <li>Calls {@code GET /api/session/init}</li>
     *   <li>Derives session keys via {@link ClientCrypto}</li>
     *   <li>Opens a WebSocket to {@code ws://host/dispatcher/{appId}}</li>
     *   <li>Sends the init message, triggering the async state push</li>
     * </ol>
     *
     * @param serverUrl base URL of the Host server (e.g., {@code http://localhost:8080})
     * @throws IOException          if the HTTP request fails
     * @throws InterruptedException if interrupted during connection
     */
    public static HostClient connect(String serverUrl) throws IOException, InterruptedException {
        LOG.info("Connecting to {}", serverUrl);

        // 1. Session init
        var http = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + SESSION_INIT_PATH))
                .GET()
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IOException("Session init failed with HTTP " + resp.statusCode() + ": " + resp.body());
        }
        Map<String, Object> initData = GSON.fromJson(resp.body(), MAP_TYPE.getType());
        String appId = (String) initData.get("appId");
        String appSKey = (String) initData.get("appSKey");
        LOG.debug("Session init: appId={}", appId);

        // 2. Build secret (RSA + PBKDF2)
        SecretContext secret = ClientCrypto.generateSecret(appSKey);

        // 3. Open WebSocket
        var sharedStates = new ViewStateMap();
        var wsBase = serverUrl.replaceFirst("^http", "ws");
        var wsUrl = wsBase + "/dispatcher/" + appId;

        var clientSession = new HostClientSession(sharedStates);
        clientSession.connect(wsUrl);

        // 4. Send init message (no requestId -- triggers async state push)
        Map<String, Object> initMsg = new HashMap<>();
        initMsg.put("secret", secret.signature());
        initMsg.put("event", List.of());
        clientSession.send(GSON.toJson(initMsg));

        LOG.info("Connected and init message sent");
        return new HostClient(serverUrl, secret, clientSession, sharedStates);
    }

    // :: Navigation

    /**
     * Sends a navigation event and blocks until the server confirms the navigation
     * (i.e., until the response with the matching {@code requestId} arrives).
     * Intermediate messages (loading states, async pushes) are discarded from the
     * caller's perspective — they are already applied to the shared {@link ViewStateMap}.
     *
     * @param path the navigation path (plain place token, e.g., {@code "login"}, {@code "home"})
     * @return the server response confirming the navigation
     */
    public HostResponse navigate(String path) throws InterruptedException, TimeoutException {
        long reqId = requestCounter.incrementAndGet();
        Map<String, Object> formData = new HashMap<>();
        formData.put("p.path", path);

        Map<String, Object> msg = new HashMap<>();
        msg.put("requestId", reqId);
        msg.put("event", List.of(HostClientSession.BROWSER_VSID + ":-1"));
        msg.put(HostClientSession.BROWSER_VSID, formData);

        session.send(GSON.toJson(msg));
        LOG.debug("navigate -> requestId={}, path={}", reqId, path);
        return awaitResponseFor(reqId);
    }

    // :: Event submission

    /**
     * Sends an event to the given view instance and returns the {@code requestId} used.
     * <p>
     * The caller is responsible for awaiting the response via {@link #awaitResponseFor(long)}
     * (which skips intermediate messages) or {@link #awaitResponse()} (which returns the
     * very next queued message).
     *
     * @param instanceId  the view instance ID (e.g., {@code "productList:1"})
     * @param eventCode   the numeric event code defined on the presenter
     * @param formData    field values for the event (may be empty)
     * @return the requestId used for this submission (use with {@link #awaitResponseFor})
     */
    public long submit(String instanceId, int eventCode, Map<String, Object> formData) {
        long reqId = requestCounter.incrementAndGet();

        Map<String, Object> msg = new HashMap<>();
        msg.put("requestId", reqId);
        msg.put("event", List.of(instanceId + ":" + eventCode));
        msg.put(instanceId, formData != null ? formData : Map.of());

        session.send(GSON.toJson(msg));
        LOG.debug("submit -> requestId={}, instanceId={}, event={}", reqId, instanceId, eventCode);
        return reqId;
    }

    // :: Response handling

    /**
     * Blocks until the next server message arrives (default 10-second timeout).
     * Returns <em>any</em> queued message — intermediate or final.
     * Use this only for the initial async push (no requestId) that the server
     * sends immediately after connection.
     *
     * @return the parsed response, never {@code null}
     * @throws TimeoutException if no response arrives within the default timeout
     */
    public HostResponse awaitResponse() throws InterruptedException, TimeoutException {
        return awaitResponse(DEFAULT_TIMEOUT);
    }

    /**
     * Blocks until the next server message arrives or the timeout elapses.
     * Returns <em>any</em> queued message — intermediate or final.
     *
     * @throws TimeoutException if no response arrives within {@code timeout}
     */
    public HostResponse awaitResponse(Duration timeout) throws InterruptedException, TimeoutException {
        HostResponse response = session.receive(timeout);
        if (response == null) {
            throw new TimeoutException("No response from Host within " + timeout.toMillis() + "ms");
        }
        return response;
    }

    /**
     * Blocks until the server message with the given {@code requestId} arrives
     * (default 10-second timeout), skipping any intermediate messages.
     * <p>
     * Intermediate messages (no {@code requestId} or a different one) are discarded
     * from the caller's perspective — they are already applied to the
     * shared {@link ViewStateMap} by the WebSocket listener.
     *
     * @throws TimeoutException if the matching response does not arrive within the default timeout
     */
    public HostResponse awaitResponseFor(long requestId) throws InterruptedException, TimeoutException {
        return awaitResponseFor(requestId, DEFAULT_TIMEOUT);
    }

    /**
     * Blocks until the server message with the given {@code requestId} arrives
     * or the timeout elapses, skipping intermediate messages.
     *
     * @throws TimeoutException if the matching response does not arrive within {@code timeout}
     */
    public HostResponse awaitResponseFor(long requestId, Duration timeout) throws InterruptedException, TimeoutException {
        HostResponse response = session.receiveFor(requestId, timeout);
        if (response == null) {
            throw new TimeoutException(
                    "No response for requestId=" + requestId + " from Host within " + timeout.toMillis() + "ms");
        }
        return response;
    }

    // :: ViewState access

    /**
     * Returns the current snapshot of the given view instance, or {@code null} if not present.
     */
    public ViewStateSnapshot viewState(String instanceId) {
        return states.get(instanceId);
    }

    /**
     * Returns all currently active ViewState snapshots.
     */
    public Collection<ViewStateSnapshot> allViewStates() {
        return states.all();
    }

    /**
     * Returns all active ViewState snapshots for the given classId.
     * <p>
     * Useful when a presenter spawns multiple instances (e.g., several {@code productList} views).
     * Matches any instanceId with the pattern {@code "classId:N"}.
     *
     * @param classId the view class ID (e.g., {@code "productList"}, {@code "cart"})
     */
    public Collection<ViewStateSnapshot> viewStatesByClass(String classId) {
        return states.allByClassId(classId);
    }

    /**
     * Creates a typed presenter client for a known {@code instanceId}.
     * <p>
     * Use this when you already know the exact instanceId from the view composition
     * (e.g., from a parent presenter's {@code contentViewId} field) rather than
     * searching by class ID.
     *
     * @param instanceId    the exact view instance ID (e.g., {@code "48b693f67410:6"})
     * @param presenterType the presenter client class to instantiate
     */
    public <T extends AbstractPresenterClient> T presenterByInstanceId(
            String instanceId, Class<T> presenterType) {
        try {
            return presenterType.getConstructor(HostClient.class, String.class)
                    .newInstance(this, instanceId);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    presenterType.getSimpleName() + " must have a public (HostClient, String) constructor", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + presenterType.getSimpleName(), e);
        }
    }

    /**
     * Returns the presenter currently shown as the top-level page by following the
     * view composition chain: {@code browser → frame → page}.
     * <p>
     * The browser ViewState ({@code 7b32e816a191:0}) holds a {@code contentViewId}
     * pointing to the frame/shell view, which in turn holds a {@code contentViewId}
     * pointing to the active page (login, home, etc.).
     *
     * @param presenterType the expected presenter type at the page level
     * @return the typed presenter, or {@link Optional#empty()} if the chain is incomplete
     */
    public <T extends AbstractPresenterClient> Optional<T> currentPagePresenter(Class<T> presenterType) {
        var browserState = states.get(BROWSER_VSID);
        if (browserState == null) return Optional.empty();
        var frameId = browserState.getString("contentViewId");
        if (frameId == null) return Optional.empty();
        var frameState = states.get(frameId);
        if (frameState == null) return Optional.empty();
        var pageId = frameState.getString("contentViewId");
        if (pageId == null) return Optional.empty();
        return Optional.of(presenterByInstanceId(pageId, presenterType));
    }

    /**
     * Instantiates one {@code presenterType} per matching instanceId using the
     * {@code (HostClient client, String vsid)} constructor convention.
     *
     * @param classId       the view class ID (e.g., {@code CartPresenterClient.CLASS_ID})
     * @param presenterType the presenter client class to instantiate
     */
    public <T extends AbstractPresenterClient> List<T> presentersByClass(
            String classId, Class<T> presenterType) {
        try {
            var ctor = presenterType.getConstructor(HostClient.class, String.class);
            var result = new ArrayList<T>();
            for (var vs : states.allByClassId(classId)) {
                result.add(ctor.newInstance(this, vs.instanceId()));
            }
            return result;
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    presenterType.getSimpleName() + " must have a public (HostClient, String) constructor", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to instantiate " + presenterType.getSimpleName(), e);
        }
    }

    /**
     * Returns an {@link Optional} containing the first active presenter client for the given classId,
     * or {@link Optional#empty()} if none is found.
     *
     * @param classId       the view class ID (e.g., {@code CartPresenterClient.CLASS_ID})
     * @param presenterType the presenter client class to instantiate
     */
    public <T extends AbstractPresenterClient> Optional<T> firstPresenterByClass(
            String classId, Class<T> presenterType) {
        try {
            var ctor = presenterType.getConstructor(HostClient.class, String.class);
            return states.allByClassId(classId).stream()
                    .findFirst()
                    .map(vs -> {
                        try {
                            return ctor.newInstance(this, vs.instanceId());
                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException("Failed to instantiate " + presenterType.getSimpleName(), e);
                        }
                    });
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(
                    presenterType.getSimpleName() + " must have a public (HostClient, String) constructor", e);
        }
    }

    /**
     * Returns the {@link SecretContext} for this session (e.g., for deciphering access tokens).
     */
    public SecretContext secretContext() {
        return secret;
    }

    // :: Dev utilities

    /**
     * Calls the {@code POST /__dev/db-reset} endpoint to reset the database to a known seed state.
     * <p>
     * Only works when the server is running with {@code server.devMode=true}.
     * Intended for scenario/integration runners that need a predictable initial state.
     *
     * @throws IOException          if the HTTP request fails
     * @throws InterruptedException if interrupted
     * @throws IllegalStateException if the server returns a non-200 status
     */
    public void resetDb() throws IOException, InterruptedException {
        var http = HttpClient.newHttpClient();
        var req = HttpRequest.newBuilder()
                .uri(URI.create(serverUrl + "/__dev/db-reset"))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new IllegalStateException(
                    "DB reset failed with HTTP " + resp.statusCode() + ": " + resp.body());
        }
        LOG.info("DB reset completed");
    }

    // :: Lifecycle

    @Override
    public void close() {
        session.close();
    }
}
