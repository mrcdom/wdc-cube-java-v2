package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.io.WebSocket;
import com.codename1.ui.CN;

/**
 * Cliente do protocolo bridge (lado Codename One) — conexão, estado e submissão de eventos.
 *
 * <p>
 * Os presenters rodam no servidor (remote.host); aqui só mantemos o mapa de ViewStates recebido
 * (vsid -> campos) e re-renderizamos a tela corrente a cada push. Single-thread como os demais shells.
 * </p>
 *
 * <p>
 * <b>Cripto adiada:</b> o handshake usa um {@code secret} placeholder não-vazio — suficiente para
 * conectar, receber estado e navegar. O login seguro (cifra da senha) entra numa fase posterior.
 * </p>
 */
public final class BridgeSession {

    /** vsid do BrowserPresenter (raiz da árvore de navegação). */
    public static final String BROWSER_VSID = "7b32e816a191:0";

    /** Listener de atualização — chamado no EDT após cada push de estado. */
    public interface Listener {
        void onUpdate(BridgeSession session);
    }

    private final String base;     // ex.: http://localhost:8080
    private final String wsBase;   // ex.: ws://localhost:8080
    private final Listener listener;

    private final Map<String, Map<String, Object>> states = new HashMap<>();
    private WebSocket ws;
    private String uri = "";
    private long requestCounter = 0;

    public BridgeSession(String base, Listener listener) {
        this.base = base;
        this.wsBase = "ws" + base.substring(4); // http:// -> ws:// (ambiente local)
        this.listener = listener;
    }

    /** Bloqueante (chamar fora do EDT): faz session/init e abre o WebSocket. */
    public void connect() {
        ConnectionRequest req = new ConnectionRequest();
        req.setUrl(base + "/api/session/init");
        req.setPost(false);
        NetworkManager.getInstance().addToQueueAndWait(req);

        byte[] data = req.getResponseData();
        if (data == null) {
            throw new RuntimeException("session/init sem corpo (HTTP " + req.getResponseCode() + ")");
        }
        Map<String, Object> init = parse(new String(data));
        String appId = str(init.get("appId"));
        if (appId == null) {
            throw new RuntimeException("session/init sem appId");
        }

        ws = WebSocket.build(wsBase + "/dispatcher/" + appId)
                .onConnect(w -> w.send("{\"secret\":\"probe-dummy\",\"event\":[]}"))
                .onTextMessage((w, msg) -> handleMessage(msg))
                .onClose((w, code, reason) -> { })
                .onError((w, ex) -> { })
                .connect();
    }

    private void handleMessage(String msg) {
        try {
            Map<String, Object> resp = parse(msg);
            Object u = resp.get("uri");
            if (u != null) {
                uri = u.toString();
            }
            Object statesObj = resp.get("states");
            if (statesObj instanceof List) {
                for (Object o : (List<?>) statesObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> st = (Map<String, Object>) o;
                        Object id = st.get("#");
                        if (id != null) {
                            states.put(id.toString(), st);
                        }
                    }
                }
            }
        } catch (Exception ignore) {
            // mensagem malformada — ignora; o estado anterior permanece
        }
        CN.callSerially(() -> listener.onUpdate(this));
    }

    /** vsid da tela corrente: browser -> contentViewId (root) -> contentViewId (tela). */
    public String currentScreenVsid() {
        Map<String, Object> browser = states.get(BROWSER_VSID);
        if (browser == null) {
            return null;
        }
        String rootVsid = str(browser.get("contentViewId"));
        Map<String, Object> root = rootVsid != null ? states.get(rootVsid) : null;
        if (root == null) {
            return rootVsid;
        }
        String screen = str(root.get("contentViewId"));
        return screen != null ? screen : rootVsid;
    }

    public Map<String, Object> state(String vsid) {
        return vsid != null ? states.get(vsid) : null;
    }

    public Map<String, Map<String, Object>> allStates() {
        return states;
    }

    public String uri() {
        return uri;
    }

    /** classId é o prefixo do vsid antes do ':'. */
    public static String classIdOf(String vsid) {
        if (vsid == null) {
            return null;
        }
        int i = vsid.indexOf(':');
        return i > 0 ? vsid.substring(0, i) : vsid;
    }

    /** Submete um evento ao servidor (plumbing para as próximas fases). */
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
            throw new RuntimeException("JSON inválido", e);
        }
    }

    private static String str(Object o) {
        return o == null ? null : o.toString();
    }
}
