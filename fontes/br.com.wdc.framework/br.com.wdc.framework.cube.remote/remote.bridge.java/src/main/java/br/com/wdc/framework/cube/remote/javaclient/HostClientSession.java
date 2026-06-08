package br.com.wdc.framework.cube.remote.javaclient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.reflect.TypeToken;

import br.com.wdc.framework.cube.remote.javaclient.model.HostResponse;
import br.com.wdc.framework.cube.remote.javaclient.model.ViewStateMap;
import br.com.wdc.framework.cube.remote.javaclient.model.ViewStateSnapshot;

/**
 * Low-level WebSocket session for the Host/Shell protocol.
 * <p>
 * Manages the WebSocket connection, message framing, JSON parsing, and an
 * inbound queue. Consumers call {@link #receive(Duration)} to block until
 * the next server message arrives.
 */
final class HostClientSession implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(HostClientSession.class);

    static final String BROWSER_VSID = "7b32e816a191:0";

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
            .create();

    private static final TypeToken<Map<String, Object>> MAP_TYPE = new TypeToken<>() {};

    private final HttpClient http;
    private WebSocket ws;
    private final LinkedBlockingQueue<HostResponse> inbound = new LinkedBlockingQueue<>();
    private final ViewStateMap viewStateMap;
    private volatile boolean closed = false;

    HostClientSession(ViewStateMap viewStateMap) {
        this.http = HttpClient.newHttpClient();
        this.viewStateMap = viewStateMap;
    }

    void connect(String wsUrl) {
        LOG.debug("Connecting to {}", wsUrl);
        this.ws = http.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(URI.create(wsUrl), new MessageListener())
                .join();
        LOG.debug("WebSocket connected");
    }

    void send(String json) {
        LOG.debug("→ {}", json);
        ws.sendText(json, true);
    }

    /**
     * Blocks until the next server message is available or the timeout elapses.
     *
     * @return the parsed response, or {@code null} on timeout
     */
    HostResponse receive(Duration timeout) throws InterruptedException {
        return inbound.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Blocks until a server message with the given {@code requestId} arrives, or the timeout elapses.
     * Intermediate messages (no requestId or different requestId) are discarded — their states are
     * already applied to the shared {@link ViewStateMap} by the WebSocket listener.
     *
     * @return the matching response, or {@code null} on timeout
     */
    HostResponse receiveFor(long requestId, Duration timeout) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (true) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0) return null;
            HostResponse r = inbound.poll(remaining, TimeUnit.MILLISECONDS);
            if (r == null) return null;
            if (r.requestId() != null && r.requestId() == requestId) return r;
            LOG.debug("Skipping intermediate message (requestId={})", r.requestId());
        }
    }

    boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        closed = true;
        if (ws != null) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
        http.close();
    }

    // :: JSON parsing

    @SuppressWarnings("unchecked")
    private HostResponse parse(String json) {
        LOG.debug("← {}", json);
        Map<String, Object> msg = GSON.fromJson(json, MAP_TYPE.getType());

        Long requestId = null;
        Object rid = msg.get("requestId");
        if (rid instanceof Number n) requestId = n.longValue();

        String uri = (String) msg.get("uri");
        String accessToken = (String) msg.get("accessToken");

        // Parse states
        List<ViewStateSnapshot> snapshots = Collections.emptyList();
        Object rawStates = msg.get("states");
        if (rawStates instanceof List<?> stateList && !stateList.isEmpty()) {
            snapshots = new ArrayList<>(stateList.size());
            for (Object entry : stateList) {
                if (entry instanceof Map<?, ?> rawMap) {
                    var stateMap = new java.util.HashMap<String, Object>();
                    ((Map<String, Object>) rawMap).forEach(stateMap::put);
                    String instanceId = String.valueOf(stateMap.remove("#"));
                    if (instanceId != null && !instanceId.equals("null")) {
                        snapshots.add(new ViewStateSnapshot(instanceId, stateMap));
                    }
                }
            }
            // Apply to shared map
            List<Object> rawList = (List<Object>) rawStates;
            viewStateMap.applyStates(rawList);
        }

        // Parse released views
        List<String> released = Collections.emptyList();
        Object rawReleased = msg.get("releasedViews");
        if (rawReleased instanceof List<?> relList) {
            released = new ArrayList<>();
            for (Object r : relList) {
                released.add(String.valueOf(r));
            }
            viewStateMap.applyReleased(released);
        }

        return new HostResponse(requestId, uri, snapshots, released, accessToken);
    }

    // :: WebSocket Listener

    private final class MessageListener implements WebSocket.Listener {

        private final StringBuilder buf = new StringBuilder();

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            buf.append(data);
            if (last) {
                String text = buf.toString();
                buf.setLength(0);
                try {
                    inbound.add(parse(text));
                } catch (Exception e) {
                    LOG.warn("Failed to parse server message: {}", text, e);
                }
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            LOG.debug("WebSocket closed: {} {}", statusCode, reason);
            closed = true;
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            LOG.warn("WebSocket error", error);
            closed = true;
        }
    }
}
