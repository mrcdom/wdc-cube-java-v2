package br.com.wdc.store.view.remote.shell.cn1.bridge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * The handshake is RSA + PBKDF2 + AES-GCM ({@link Cn1Crypto}): the {@code secret} of the 1st WS
 * message carries the session key, and sensitive values (e.g. the password) are enciphered with it.
 * </p>
 */
public final class BridgeSession {

    /** BrowserPresenter's vsid (root of the navigation tree). */
    public static final String BROWSER_VSID = "7b32e816a191:0";

    /** Preference (client-only) with the last navigated path, to restore the screen on reopening. */
    private static final String INTENT_PREF = "wdc.lastIntent";

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

    private final Map<String, Map<String, Object>> scopes = new HashMap<>();
    private final Cn1ClientStorage clientStorage = new Cn1ClientStorage();
    private WebSocket ws;
    private Cn1Crypto crypto;
    private String uri = "";
    private long requestCounter = 0;

    public BridgeSession(String base, Listener listener) {
        this.base = base;
        this.wsBase = "ws" + base.substring(4); // http:// -> ws:// (local environment)
        this.listener = listener;
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
        String appId = str(init.get("appId"));
        String appSKey = str(init.get("appSKey"));
        if (appId == null || appSKey == null) {
            throw new RuntimeException("session/init without appId/appSKey");
        }

        // Real handshake (RSA + PBKDF2 + AES-GCM): enables secure login.
        crypto = Cn1Crypto.generate(appSKey);
        final String secret = crypto.signature();

        ws = WebSocket.build(wsBase + "/dispatcher/" + appId)
                .onConnect(w -> w.send(bootstrapMessage(secret)))
                .onTextMessage((w, msg) -> handleMessage(msg))
                .onClose((w, code, reason) -> { })
                .onError((w, ex) -> { })
                .connect();
    }

    /** 1st WS message: secret + the client storage (enciphered) for the server to populate the scopes. */
    private String bootstrapMessage(String secret) {
        Map<String, Object> boot = new HashMap<>();
        boot.put("secret", secret);
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

    /** Enciphers a sensitive value (e.g.: password) with the session AES key, to send to the server. */
    public String cipher(String plaintext) {
        return crypto.encipher(plaintext);
    }

    /** Submits an event to the server. */
    public void submit(String vsid, int eventCode, Map<String, Object> form) {
        long id = ++requestCounter;
        Map<String, Object> msg = new HashMap<>();
        msg.put("requestId", id);
        List<String> ev = new ArrayList<>();
        ev.add(vsid + ":" + eventCode);
        msg.put("event", ev);
        msg.put(vsid, form != null ? form : new HashMap<>());
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
