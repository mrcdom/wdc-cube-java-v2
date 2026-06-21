package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

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

    /** Preferência (client-only) com o último path navegado, para restaurar a tela ao reabrir. */
    private static final String INTENT_PREF = "wdc.lastIntent";

    /** Listener de atualização — chamado no EDT após cada push de estado. */
    public interface Listener {
        void onUpdate(BridgeSession session);
    }

    private final String base;     // ex.: http://localhost:8080
    private final String wsBase;   // ex.: ws://localhost:8080
    private final Listener listener;

    private final Map<String, Map<String, Object>> states = new HashMap<>();
    /** vsids cujos ViewStates vieram no último push — para o app despachar doUpdate só neles. */
    private final List<String> lastReceived = new ArrayList<>();
    private final Cn1ClientStorage clientStorage = new Cn1ClientStorage();
    private WebSocket ws;
    private Cn1Crypto crypto;
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
        String appSKey = str(init.get("appSKey"));
        if (appId == null || appSKey == null) {
            throw new RuntimeException("session/init sem appId/appSKey");
        }

        // Handshake real (RSA + PBKDF2 + AES-GCM): habilita login seguro.
        crypto = Cn1Crypto.generate(appSKey);
        final String secret = crypto.signature();

        ws = WebSocket.build(wsBase + "/dispatcher/" + appId)
                .onConnect(w -> w.send(bootstrapMessage(secret)))
                .onTextMessage((w, msg) -> handleMessage(msg))
                .onClose((w, code, reason) -> { })
                .onError((w, ex) -> { })
                .connect();
    }

    /** 1ª mensagem WS: secret + o client storage (cifrado) para o servidor popular os escopos. */
    private String bootstrapMessage(String secret) {
        Map<String, Object> boot = new HashMap<>();
        boot.put("secret", secret);
        boot.put("event", new ArrayList<>());
        Map<String, Object> storage = clientStorage.bootstrap(crypto);
        if (!storage.isEmpty()) {
            boot.put("storage", storage);
        }
        // Restaura a última tela: reenvia o path salvo como p.path do browser (createApp → safeGo).
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
            Object statesObj = resp.get("states");
            if (statesObj instanceof List) {
                lastReceived.clear();
                for (Object o : (List<?>) statesObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> st = (Map<String, Object>) o;
                        Object id = st.get("#");
                        if (id != null) {
                            states.put(id.toString(), st);
                            lastReceived.add(id.toString());
                        }
                    }
                }
            }
            applyStorageDelta(resp.get("storage"));
        } catch (Exception ignore) {
            // mensagem malformada — ignora; o estado anterior permanece
        }
        CN.callSerially(() -> listener.onUpdate(this));
    }

    /** Salva o último path navegado (exceto login) para restaurar a tela ao reabrir o app. */
    private void persistIntent(String u) {
        if (u == null || u.isEmpty() || u.contains("login")) {
            return;
        }
        Preferences.set(INTENT_PREF, u);
    }

    /** Aplica os deltas de storage do servidor: decifra os valores e grava (ou remove se {@code null}). */
    private void applyStorageDelta(Object storageObj) {
        if (!(storageObj instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> delta = (Map<String, Object>) storageObj;
        applyScope(Cn1ClientStorage.SESSION, delta.get(Cn1ClientStorage.SESSION));
        applyScope(Cn1ClientStorage.PERSISTENT, delta.get(Cn1ClientStorage.PERSISTENT));
        applyScope(Cn1ClientStorage.PERSISTENT_SECURE, delta.get(Cn1ClientStorage.PERSISTENT_SECURE));
    }

    private void applyScope(String scope, Object scopeObj) {
        if (!(scopeObj instanceof Map)) {
            return;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> m = (Map<String, Object>) scopeObj;
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof String && !((String) v).isEmpty()) {
                String plain = crypto.decipher((String) v);
                if (plain != null) {
                    clientStorage.apply(scope, e.getKey(), plain);
                }
            } else {
                clientStorage.apply(scope, e.getKey(), null); // null/vazio = remoção
            }
        }
    }

    /**
     * vsid da tela corrente: parte do browser e <b>desce a cadeia {@code contentViewId} até a
     * folha</b>. O servidor aninha as telas (browser → root → home → produto/carrinho/recibo),
     * então a tela "atual" é a view mais profunda sem {@code contentViewId}.
     */
    public String currentScreenVsid() {
        Map<String, Object> browser = states.get(BROWSER_VSID);
        if (browser == null) {
            return null;
        }
        String vsid = str(browser.get("contentViewId"));
        for (int depth = 0; depth < 16 && vsid != null; depth++) {
            Map<String, Object> st = states.get(vsid);
            String next = st != null ? str(st.get("contentViewId")) : null;
            if (next == null) {
                break;
            }
            vsid = next;
        }
        return vsid;
    }

    public Map<String, Object> state(String vsid) {
        return vsid != null ? states.get(vsid) : null;
    }

    /** vsids cujos ViewStates chegaram no último push (as views "dirty" do servidor). */
    public List<String> lastReceived() {
        return lastReceived;
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

    /** Cifra um valor sensível (ex.: senha) com a chave AES da sessão, para enviar ao servidor. */
    public String cipher(String plaintext) {
        return crypto.encipher(plaintext);
    }

    /** Submete um evento ao servidor. */
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
