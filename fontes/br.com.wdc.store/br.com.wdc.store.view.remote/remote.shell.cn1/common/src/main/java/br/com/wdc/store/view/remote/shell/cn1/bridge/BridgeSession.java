package br.com.wdc.store.view.remote.shell.cn1.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.Preferences;
import com.codename1.io.WebSocket;
import com.codename1.ui.CN;

/**
 * Bridge protocol client (Codename One side) — connection, scopes and event submission.
 *
 * <p>
 * The presenters run on the server (remote.host); here we only keep the map of received scopes
 * (vsid -> fields) and re-render the current screen on each push. Single-threaded like the other shells.
 * </p>
 *
 * <p>
 * Each connection starts with the handshake of protocol {@value #PROTOCOL} ({@link Cn1Crypto}): the first message
 * is {@code {"protocol":1,"clientKey":…}} and nothing else goes out until the host answers with its
 * {@code serverKey}; both ends then hold the connection's AES-256 key, which ciphers the storage values and
 * sensitive parameters (e.g. the password). Messages submitted meanwhile wait, and go out after the bootstrap
 * message. A host that refuses the version ({@code unsupported-protocol}, close {@value #CLOSE_UNSUPPORTED_PROTOCOL})
 * leaves the session out of date: nothing more is sent and the shell is told ({@code onOutdated}).
 * </p>
 *
 * <p>
 * Once the handshake is done, the keep-alive ({@code {"ping":true}}) goes every {@link #PING_INTERVAL_MS}: the server
 * extends the session's life and answers with the views it released ({@code releasedViews}), which the push
 * discards.
 * </p>
 */
public final class BridgeSession {

    /** BrowserPresenter's vsid (root of the navigation tree). */
    public static final String BROWSER_VSID = "7b32e816a191:0";

    /** Preference (client-only) with the last navigated path, to restore the screen on reopening. */
    private static final String INTENT_PREF = "wdc.lastIntent";

    /** How often the keep-alive goes to the server. */
    private static final long PING_INTERVAL_MS = 15000L;

    /** The version of the remote protocol this client speaks. */
    public static final int PROTOCOL = 1;

    /** Close code of a host that does not speak {@link #PROTOCOL}. */
    public static final int CLOSE_UNSUPPORTED_PROTOCOL = 4002;

    /**
     * Push notification — on the EDT, with the vsids of the views <b>received</b> (dirty on the server) and
     * <b>released</b> in this push. Each push passes its own lists (no shared state),
     * so bursts of pushes do not overwrite each other.
     */
    public interface Listener {
        void onPush(List<String> received, List<String> released);
    }

    private final String base;     // e.g.: http://localhost:8080
    private final String wsBase;   // e.g.: ws://localhost:8080
    private final Listener listener;
    /** Runs on the EDT, once, when the host refuses this client's protocol version. */
    private final Runnable onOutdated;

    private final Map<String, Map<String, Object>> scopes = new HashMap<>();
    private final Cn1ClientStorage clientStorage = new Cn1ClientStorage();
    private WebSocket ws;
    private String appId;
    /** This connection's key agreement, until the host answers it. */
    private Cn1Crypto.Handshake handshake;
    /** The connection key; {@code null} until the handshake is answered. */
    private volatile Cn1Crypto crypto;
    /** Whether the bootstrap message went out (EDT): until then, submitted messages wait in {@link #pending}. */
    private volatile boolean ready;
    private final List<Map<String, Object>> pending = new ArrayList<>();
    private volatile boolean outdated;
    private String uri = "";
    private long requestCounter = 0;
    private Timer pinger;

    public BridgeSession(String base, Listener listener, Runnable onOutdated) {
        this.base = base;
        this.wsBase = "ws" + base.substring(4); // http:// -> ws:// (local environment)
        this.listener = listener;
        this.onOutdated = onOutdated;
    }

    /** Blocking (call off the EDT): performs session/init and opens the WebSocket. */
    public void connect() {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(base + "/api/session/init");
        req.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(req);

        byte[] data = req.getResponseData();
        if (data == null) {
            throw new RuntimeException("session/init without body (HTTP " + req.getResponseCode() + ")");
        }
        Map<String, Object> init = parse(new String(data));
        appId = str(init.get("appId"));
        if (appId == null) {
            throw new RuntimeException("session/init without appId");
        }

        ws = WebSocket.build(wsBase + "/dispatcher/" + appId)
                .onConnect(w -> sendHandshake(w))
                .onTextMessage((w, msg) -> {
                    if (crypto == null) {
                        handleHandshakeAnswer(w, msg);
                    } else {
                        handleMessage(msg);
                    }
                })
                .onClose((w, code, reason) -> {
                    stopPinging();
                    if (code == CLOSE_UNSUPPORTED_PROTOCOL) {
                        markOutdated();
                    }
                })
                .onError((w, ex) -> stopPinging())
                .connect();
    }

    /** The first message of the connection: this client's version and ephemeral key, and nothing else. */
    private void sendHandshake(WebSocket w) {
        stopPinging();
        ready = false;
        crypto = null;
        handshake = Cn1Crypto.newHandshake();
        Map<String, Object> hello = new HashMap<>();
        hello.put("protocol", PROTOCOL);
        hello.put("clientKey", handshake.clientKey());
        w.send(JSONParser.mapToJson(hello));
    }

    /**
     * The host's answer to the handshake: its key, from which the connection key is derived; then (on the EDT) the
     * bootstrap message, the messages that waited, and the keep-alive. A refusal of the version leaves the session
     * out of date; any other answer closes the connection.
     */
    private void handleHandshakeAnswer(WebSocket w, String msg) {
        Map<String, Object> answer;
        try {
            answer = parse(msg);
        } catch (Exception e) {
            answer = new HashMap<>();
        }
        if ("unsupported-protocol".equals(str(answer.get("error")))) {
            markOutdated();
            return;
        }
        Object serverKey = answer.get("serverKey");
        try {
            if (!(serverKey instanceof String) || handshake == null) {
                throw new IllegalArgumentException("not a handshake answer");
            }
            crypto = handshake.agree((String) serverKey, appId);
        } catch (Exception e) {
            w.close();
            return;
        } finally {
            handshake = null;
        }
        CN.callSerially(() -> {
            if (outdated) {
                return;
            }
            ws = w;
            w.send(bootstrapMessage());
            ready = true;
            for (Map<String, Object> m : pending) {
                w.send(JSONParser.mapToJson(m));
            }
            pending.clear();
            startPinging();
        });
    }

    /** The host does not speak this client's protocol: nothing more goes out, and the shell is told once. */
    private void markOutdated() {
        if (outdated) {
            return;
        }
        outdated = true;
        stopPinging();
        CN.callSerially(() -> {
            pending.clear();
            if (onOutdated != null) {
                onOutdated.run();
            }
        });
    }

    /** Whether the host refused this client's protocol version: the app is out of date and does not reconnect. */
    public boolean isOutdated() {
        return outdated;
    }

    /** Sends the keep-alive on the EDT, like every other message, while the socket is open. */
    private synchronized void startPinging() {
        stopPinging();
        pinger = new Timer();
        pinger.schedule(new TimerTask() {
            @Override
            public void run() {
                CN.callSerially(() -> {
                    if (ws != null && ready && !outdated) {
                        ws.send("{\"ping\":true}");
                    }
                });
            }
        }, PING_INTERVAL_MS, PING_INTERVAL_MS);
    }

    private synchronized void stopPinging() {
        if (pinger != null) {
            pinger.cancel();
            pinger = null;
        }
    }

    /**
     * The bootstrap message, the first after the handshake: the client storage (ciphered with the connection key)
     * for the server to populate the scopes.
     */
    private String bootstrapMessage() {
        Map<String, Object> boot = new HashMap<>();
        boot.put("event", new ArrayList<>());
        Map<String, Object> storage = clientStorage.bootstrap(crypto);
        if (!storage.isEmpty()) {
            boot.put("storage", storage);
        }
        // Restores the last screen: resends the saved path as the browser's p.path (createApp → safeGo).
        String savedPath = Preferences.get(INTENT_PREF, "");
        if (savedPath != null && !savedPath.isEmpty()) {
            Map<String, Object> browserForm = new HashMap<>();
            browserForm.put("p.path", savedPath);
            boot.put(BROWSER_VSID, browserForm);
        }
        return JSONParser.mapToJson(boot);
    }

    private void handleMessage(String msg) {
        try {
            Map<String, Object> resp = parse(msg);
            Object u = resp.get("uri");
            if (u != null) {
                uri = u.toString();
                persistIntent(uri);
            }
            List<String> received = new ArrayList<>();
            Object scopesObj = resp.get("scopes");
            if (scopesObj instanceof List) {
                for (Object o : (List<?>) scopesObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> scope = (Map<String, Object>) o;
                        Object id = scope.get("#");
                        if (id != null) {
                            scopes.put(id.toString(), scope);
                            received.add(id.toString());
                        }
                    }
                }
            }

            List<String> released = new ArrayList<>();
            Object releasedObj = resp.get("releasedViews");
            if (releasedObj instanceof List) {
                for (Object o : (List<?>) releasedObj) {
                    if (o != null) {
                        String id = o.toString();
                        scopes.remove(id);
                        released.add(id);
                    }
                }
            }

            applyStorageDelta(resp.get("storage"));
            CN.callSerially(() -> listener.onPush(received, released));
        } catch (Exception ignore) {
            // malformed message — ignored; the previous scopes remain
        }
    }

    /** Saves the last navigated path (except login) to restore the screen when reopening the app. */
    private void persistIntent(String u) {
        if (u == null || u.isEmpty() || u.contains("login")) {
            return;
        }
        Preferences.set(INTENT_PREF, u);
    }

    /** Applies the server's storage deltas: deciphers the values and stores them (or removes if {@code null}). */
    private void applyStorageDelta(Object storageObj) {
        if (!(storageObj instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> delta = (Map<String, Object>) storageObj;
        applyStorageScope(Cn1ClientStorage.SESSION, delta.get(Cn1ClientStorage.SESSION));
        applyStorageScope(Cn1ClientStorage.PERSISTENT, delta.get(Cn1ClientStorage.PERSISTENT));
        applyStorageScope(Cn1ClientStorage.PERSISTENT_SECURE, delta.get(Cn1ClientStorage.PERSISTENT_SECURE));
    }

    private void applyStorageScope(String storageScope, Object entriesObj) {
        if (!(entriesObj instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) entriesObj;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String && !((String) v).isEmpty()) {
                String plain = crypto.decipher((String) v);
                if (plain != null) {
                    clientStorage.apply(storageScope, e.getKey(), plain);
                }
            } else {
                clientStorage.apply(storageScope, e.getKey(), null); // null/empty = removal
            }
        }
    }

    /**
     * vsid of the current screen: starts from the browser and <b>walks down the {@code contentViewId}
     * chain to the leaf</b>. The server nests the screens (browser → root → home → product/cart/receipt),
     * so the "current" screen is the deepest view without {@code contentViewId}.
     */
    public String currentScreenVsid() {
        Map<String, Object> browser = scopes.get(BROWSER_VSID);
        if (browser == null) {
            return null;
        }
        String vsid = str(browser.get("contentViewId"));
        for (int depth = 0; depth < 16 && vsid != null; depth++) {
            Map<String, Object> scope = scopes.get(vsid);
            String next = scope != null ? str(scope.get("contentViewId")) : null;
            if (next == null) {
                break;
            }
            vsid = next;
        }
        return vsid;
    }

    public Map<String, Object> scope(String vsid) {
        return vsid != null ? scopes.get(vsid) : null;
    }

    public Map<String, Map<String, Object>> allScopes() {
        return scopes;
    }

    public String uri() {
        return uri;
    }

    /** classId is the vsid prefix before the ':'. */
    public static String classIdOf(String vsid) {
        if (vsid == null) {
            return null;
        }
        int i = vsid.indexOf(':');
        return i > 0 ? vsid.substring(0, i) : vsid;
    }

    /**
     * Ciphers a sensitive value (e.g.: password) with the connection key, to send to the server.
     *
     * @throws IllegalStateException before the handshake is answered (no view is drawn before it)
     */
    public String cipher(String plaintext) {
        Cn1Crypto c = crypto;
        if (c == null) {
            throw new IllegalStateException("No connection key: the handshake is not done");
        }
        return c.encipher(plaintext);
    }

    /** Submits an event to the server (on the EDT); before the bootstrap message went out, it waits. */
    public void submit(String vsid, int eventCode, Map<String, Object> form) {
        if (outdated) {
            return;
        }
        long id = ++requestCounter;
        Map<String, Object> msg = new HashMap<>();
        msg.put("requestId", id);
        List<String> ev = new ArrayList<>();
        ev.add(vsid + ":" + eventCode);
        msg.put("event", ev);
        msg.put(vsid, form != null ? form : new HashMap<>());
        if (!ready) {
            pending.add(msg);
            return;
        }
        ws.send(JSONParser.mapToJson(msg));
    }

    // -- util --

    private static Map<String, Object> parse(String json) {
        try {
            return JSONParser.parseJSON(json);
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON"
, e);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
