package br.com.wdc.cube.backend.cluster;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import br.com.wdc.framework.commons.log.Log;
import io.javalin.config.JavalinConfig;
import io.javalin.websocket.WsConnectContext;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;
import io.micrometer.core.instrument.Metrics;

/**
 * The backend instances' own mesh: each instance keeps a WebSocket open to every other one ({@link #start}, with their
 * addresses) and sends on it what the others must know ({@link #publish}: a type and an id); each receives, on the
 * connections the others open to it ({@link #configure} adds the route, {@link #PATH}), what they send ({@link #on}).
 * <p>
 * <b>Who may connect.</b> Both ends prove they hold the key (derived from the configuration's secret,
 * {@link ClusterKey}): the receiving end sends a random challenge, the connecting end answers with an HMAC-SHA256 of it
 * under the key and its own challenge, the receiving end answers that one. Nothing is read, nor sent, before that. The
 * messages themselves travel as they are: between instances on another network than a private one, use {@code wss://}.
 * <p>
 * <b>What may be missed.</b> A message published while the connection to a peer is down waits for it (the last
 * {@value #MAX_PENDING}), and the peer, when the connection comes back, also takes it as having missed something
 * ({@link #onMissed}) and drops what it holds: a message lost on a connection that broke before anyone noticed costs
 * nothing. An address that turns out to be this same instance (one list for every instance, each in it) is left alone.
 */
public final class ClusterMesh implements AutoCloseable {

    /** The route the other instances connect to. */
    public static final String PATH = "/cluster";

    /** The version of these messages. */
    public static final int PROTOCOL = 1;

    /** The connection reached this same instance. */
    static final int CLOSE_SELF = 4000;

    /** The other end did not prove the key, or speaks another version. */
    static final int CLOSE_REFUSED = 4003;

    private static final int MAX_PENDING = 1000;
    private static final String LABEL = "wdc-store/cluster/v1";

    private static final Log LOG = Log.getLogger(ClusterMesh.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Gson GSON = new Gson();

    /**
     * A dropped connection is opened again after {@code firstRetry}, doubling up to {@code maxRetry}; each connection is
     * pinged every {@code ping}, and taken as dropped after three pings without an answer.
     */
    public record Timing(Duration firstRetry, Duration maxRetry, Duration ping) {

        public static final Timing DEFAULT = new Timing(Duration.ofSeconds(1), Duration.ofSeconds(30),
                Duration.ofSeconds(20));
    }

    private final byte[] key;
    private final String nodeId;
    private final Timing timing;

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<Consumer<String>>> handlers = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<Runnable> missedHandlers = new CopyOnWriteArrayList<>();

    /** The connections other instances opened to this one, by WebSocket session. */
    private final ConcurrentHashMap<String, Incoming> incoming = new ConcurrentHashMap<>();

    /** The connections this instance opens to the others. */
    private final CopyOnWriteArrayList<PeerLink> links = new CopyOnWriteArrayList<>();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ScheduledExecutorService pinger = Executors
            .newSingleThreadScheduledExecutor(Thread.ofVirtual().name("cluster-ping").factory());

    private volatile boolean closed;

    /** @param key the key every instance of the mesh shares */
    public ClusterMesh(byte[] key) {
        this(key, UUID.randomUUID().toString(), Timing.DEFAULT);
    }

    /**
     * @param key    the key every instance of the mesh shares
     * @param nodeId this instance's name in the mesh, new for each process
     * @param timing how soon a dropped connection is opened again, and how often the connections are pinged
     */
    public ClusterMesh(byte[] key, String nodeId, Timing timing) {
        this.key = key.clone();
        this.nodeId = nodeId;
        this.timing = timing;
    }

    public String nodeId() {
        return nodeId;
    }

    // :: Use

    /** Calls {@code handler} with the id of each message of {@code type} another instance sends. */
    public void on(String type, Consumer<String> handler) {
        handlers.computeIfAbsent(type, t -> new CopyOnWriteArrayList<>()).add(handler);
    }

    /** Calls {@code handler} each time another instance connects: it may have published something while it could not. */
    public void onMissed(Runnable handler) {
        missedHandlers.add(handler);
    }

    /** Tells every other instance; one not connected now gets it when it is. */
    public void publish(String type, String id) {
        Metrics.counter("store.cluster.messages.sent", "type", type).increment();
        var message = new LinkedHashMap<String, Object>();
        message.put("type", type);
        message.put("id", id);
        var text = GSON.toJson(message);
        for (var link : links) {
            link.send(text);
        }
    }

    /** The other instances whose connection to this one proved the key. */
    public Set<String> connectedPeers() {
        var peers = new HashSet<String>();
        for (var connection : incoming.values()) {
            if (connection.peer != null) {
                peers.add(connection.peer);
            }
        }
        return peers;
    }

    /** The connections from this instance that proved the key at both ends. */
    public int readyLinks() {
        return (int) links.stream()
                .filter(link -> link.ready)
                .count();
    }

    // :: Life

    /** Adds the route other instances connect to. */
    public void configure(JavalinConfig config) {
        config.routes.ws(PATH, ws -> {
            ws.onConnect(this::accept);
            ws.onMessage(this::receive);
            ws.onClose(ctx -> {
                var connection = incoming.remove(ctx.sessionId());
                if (connection != null && connection.peer != null) {
                    LOG.info("Peer {} disconnected", connection.peer);
                }
            });
            ws.onError(ctx -> incoming.remove(ctx.sessionId()));
        });
    }

    /**
     * Opens, and keeps open, a connection to each of {@code peers} ({@code ws://host:port}; this instance among them is
     * left alone).
     */
    public void start(List<String> peers) {
        for (var address : peers) {
            var link = new PeerLink(peerUri(address));
            links.add(link);
            link.start();
        }
        var every = timing.ping().toMillis();
        pinger.scheduleWithFixedDelay(() -> links.forEach(PeerLink::ping), every, every, TimeUnit.MILLISECONDS);
        LOG.info("Cluster node {}: connecting to {}", nodeId, String.join(", ", peers));
    }

    @Override
    public void close() {
        closed = true;
        pinger.shutdownNow();
        links.forEach(PeerLink::stop);
        links.clear();
        http.shutdownNow();
    }

    /** The WebSocket address of a peer: {@code ws://} or {@code wss://}, with {@link #PATH} when it names no path. */
    static URI peerUri(String address) {
        var uri = URI.create(address.trim());
        if (!"ws".equals(uri.getScheme()) && !"wss".equals(uri.getScheme())) {
            throw new IllegalArgumentException("Not a ws:// or wss:// address: " + address);
        }
        return uri.getPath() == null || uri.getPath().isEmpty() || uri.getPath().equals("/") ? uri.resolve(PATH) : uri;
    }

    // :: Proofs

    private String prove(String role, String challenge, String node) {
        try {
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            var bytes = mac.doFinal((LABEL + "\n" + role + "\n" + challenge + "\n" + node)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(bytes);
        } catch (GeneralSecurityException e) {
            throw new AssertionError("HMAC-SHA256 not available", e);
        }
    }

    private boolean proven(String proof, String role, String challenge, String node) {
        return proof != null && MessageDigest.isEqual(proof.getBytes(StandardCharsets.UTF_8),
                prove(role, challenge, node).getBytes(StandardCharsets.UTF_8));
    }

    private static String random() {
        var bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private static String text(JsonObject message, String field) {
        var value = message.get(field);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static String json(Object... nameValue) {
        var message = new LinkedHashMap<String, Object>();
        for (int i = 0; i < nameValue.length; i += 2) {
            message.put((String) nameValue[i], nameValue[i + 1]);
        }
        return GSON.toJson(message);
    }

    // :: Connections from the others

    private static final class Incoming {

        final String challenge;
        volatile String peer;

        Incoming(String challenge) {
            this.challenge = challenge;
        }
    }

    private void accept(WsConnectContext ctx) {
        var challenge = random();
        incoming.put(ctx.sessionId(), new Incoming(challenge));
        ctx.send(json("protocol", PROTOCOL, "node", nodeId, "challenge", challenge));
    }

    private void receive(WsMessageContext ctx) {
        var connection = incoming.get(ctx.sessionId());
        if (connection == null) {
            refuse(ctx, "unknown connection");
            return;
        }
        JsonObject message;
        try {
            message = JsonParser.parseString(ctx.message()).getAsJsonObject();
        } catch (RuntimeException e) {
            refuse(ctx, "not JSON");
            return;
        }

        if (connection.peer == null) {
            hello(ctx, connection, message);
            return;
        }

        var type = text(message, "type");
        var id = text(message, "id");
        if (type == null || id == null) {
            return;
        }
        Metrics.counter("store.cluster.messages.received", "type", type).increment();
        var typeHandlers = handlers.get(type);
        if (typeHandlers != null) {
            for (var handler : typeHandlers) {
                runHandler(type, () -> handler.accept(id));
            }
        }
    }

    /** The first message proves the key; nothing else is read before it. */
    private void hello(WsMessageContext ctx, Incoming connection, JsonObject message) {
        var node = text(message, "node");
        if (node == null) {
            refuse(ctx, "no node");
            return;
        }
        if (!String.valueOf(PROTOCOL).equals(text(message, "protocol"))) {
            refuse(ctx, "protocol " + text(message, "protocol"));
            return;
        }
        if (node.equals(nodeId)) {
            ctx.closeSession(CLOSE_SELF, "self");
            return;
        }
        if (!proven(text(message, "proof"), "client", connection.challenge, node)) {
            refuse(ctx, "wrong proof from " + node);
            return;
        }
        var theirs = text(message, "challenge");
        if (theirs == null) {
            refuse(ctx, "no challenge from " + node);
            return;
        }
        connection.peer = node;
        // while it could not reach this instance, it published nothing here: drop what may be stale before answering,
        // so that once the other end is ready, this one has already done it
        for (var handler : missedHandlers) {
            runHandler("missed", handler);
        }
        ctx.send(json("proof", prove("server", theirs, nodeId)));
        LOG.info("Peer {} connected", node);
    }

    private void refuse(WsContext ctx, String why) {
        LOG.warn("Cluster connection refused: {}", why);
        incoming.remove(ctx.sessionId());
        ctx.closeSession(CLOSE_REFUSED, "refused");
    }

    private static void runHandler(String type, Runnable handler) {
        try {
            handler.run();
        } catch (RuntimeException e) {
            LOG.error("Handling cluster message {}", type, e);
        }
    }

    // :: Connections to the others

    /**
     * The connection to one peer, opened again (1 s, doubling up to 30 s) whenever it drops. Messages go out in order,
     * from one thread; until both ends proved the key they wait (the last {@value #MAX_PENDING}).
     */
    private final class PeerLink {

        private final URI uri;
        private final ArrayDeque<String> pending = new ArrayDeque<>();
        private final ExecutorService sender = Executors
                .newSingleThreadExecutor(Thread.ofVirtual().name("cluster-send").factory());

        volatile boolean ready;
        private volatile Connection current;
        private volatile boolean stopped;
        private Thread loop;

        PeerLink(URI uri) {
            this.uri = uri;
        }

        void start() {
            loop = Thread.ofVirtual()
                    .name("cluster-link")
                    .start(this::run);
        }

        void stop() {
            stopped = true;
            var connection = current;
            if (connection != null && connection.socket != null) {
                connection.socket.abort();
            }
            if (loop != null) {
                loop.interrupt();
            }
            sender.shutdownNow();
        }

        private void run() {
            var delay = timing.firstRetry().toMillis();
            var failures = 0;
            while (!closed && !stopped) {
                var connection = new Connection(this);
                current = connection;
                try {
                    http.newWebSocketBuilder()
                            .buildAsync(uri, connection)
                            .get(10, TimeUnit.SECONDS);
                    int code = connection.ended.get();
                    if (code == CLOSE_SELF) {
                        LOG.info("Cluster peer {} is this instance", uri);
                        return;
                    }
                    if (code == CLOSE_REFUSED) {
                        LOG.warn("Cluster peer {} refused this instance: is the secret the same?", uri);
                    }
                    if (connection.wasReady) {
                        LOG.info("Cluster peer {} disconnected", uri);
                        delay = timing.firstRetry().toMillis();
                        failures = 0;
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    if (failures++ == 0) {
                        LOG.warn("Cluster peer {} not reachable, trying again: {}", uri,
                                e.getCause() != null ? e.getCause() : e);
                    }
                }
                synchronized (this) {
                    ready = false;
                }
                if (connection.socket != null) {
                    connection.socket.abort();
                }
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                delay = Math.min(delay * 2, timing.maxRetry().toMillis());
            }
        }

        void send(String message) {
            synchronized (this) {
                if (!ready) {
                    pending.addLast(message);
                    if (pending.size() > MAX_PENDING) {
                        pending.removeFirst();
                    }
                    return;
                }
                var connection = current;
                if (connection == null || connection.socket == null) {
                    return;
                }
                sender.execute(() -> transmit(connection, message));
            }
        }

        /** A send that fails ends the connection at once: the next messages wait for the new one. */
        void transmit(Connection connection, String message) {
            try {
                connection.socket.sendText(message, true)
                        .get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LOG.debug("Sending to cluster peer {}", uri, e);
                synchronized (this) {
                    if (current == connection) {
                        ready = false;
                    }
                }
                connection.end(-1);
            }
        }

        /** Both ends proved the key: what waited goes out, in order. */
        void connected(Connection connection) {
            synchronized (this) {
                if (current != connection) {
                    return;
                }
                connection.wasReady = true;
                connection.lastPong = System.currentTimeMillis();
                ready = true;
                while (!pending.isEmpty()) {
                    var waiting = pending.removeFirst();
                    sender.execute(() -> transmit(connection, waiting));
                }
            }
            Metrics.counter("store.cluster.connections").increment();
            LOG.info("Cluster peer {} connected ({})", uri, connection.serverNode);
        }

        void ping() {
            var connection = current;
            if (connection == null || connection.socket == null || !ready) {
                return;
            }
            if (System.currentTimeMillis() - connection.lastPong > 3 * timing.ping().toMillis()) {
                LOG.warn("Cluster peer {} stopped answering", uri);
                connection.end(-1);
                return;
            }
            sender.execute(() -> {
                try {
                    connection.socket.sendPing(ByteBuffer.allocate(0))
                            .get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    connection.end(-1);
                }
            });
        }
    }

    /** One WebSocket to a peer, from opening to its end ({@code ended}, with the close code; {@code -1} on failure). */
    private final class Connection implements WebSocket.Listener {

        final CompletableFuture<Integer> ended = new CompletableFuture<>();
        final String challenge = random();
        private final PeerLink link;
        private final StringBuilder text = new StringBuilder();

        volatile WebSocket socket;
        volatile long lastPong;
        volatile boolean wasReady;
        volatile String serverNode = "";

        Connection(PeerLink link) {
            this.link = link;
        }

        void end(int code) {
            ended.complete(code);
            var ws = socket;
            if (ws != null) {
                ws.abort();
            }
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            socket = webSocket;
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            text.append(data);
            if (last) {
                var message = text.toString();
                text.setLength(0);
                try {
                    answer(webSocket, JsonParser.parseString(message).getAsJsonObject());
                } catch (RuntimeException e) {
                    LOG.warn("Cluster peer {}: unreadable message", link.uri, e);
                    end(-1);
                }
            }
            webSocket.request(1);
            return null;
        }

        private void answer(WebSocket webSocket, JsonObject message) {
            var challengeOfPeer = text(message, "challenge");
            if (challengeOfPeer != null) {
                // the peer's challenge: prove the key, and challenge it in turn
                var node = text(message, "node");
                serverNode = node == null ? "" : node;
                if (serverNode.equals(nodeId)) {
                    end(CLOSE_SELF);
                    return;
                }
                var hello = json("protocol", PROTOCOL, "node", nodeId, "proof",
                        prove("client", challengeOfPeer, nodeId), "challenge", challenge);
                link.sender.execute(() -> link.transmit(this, hello));
            } else if (message.has("proof") && !wasReady) {
                if (!proven(text(message, "proof"), "server", challenge, serverNode)) {
                    LOG.warn("Cluster peer {} did not prove the key", link.uri);
                    end(CLOSE_REFUSED);
                    return;
                }
                link.connected(this);
            }
        }

        @Override
        public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
            lastPong = System.currentTimeMillis();
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            ended.complete(statusCode);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            ended.complete(-1);
        }
    }
}
