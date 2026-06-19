package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.Map;

import com.codename1.io.ConnectionRequest;
import com.codename1.io.JSONParser;
import com.codename1.io.NetworkManager;
import com.codename1.components.SpanLabel;
import com.codename1.io.WebSocket;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Form;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Fase 0b — probe do protocolo bridge portado para Codename One (APIs core: ConnectionRequest,
 * JSONParser, WebSocket). Reproduz exatamente o probe Java puro da Fase 0a:
 *
 *   1. GET /api/session/init  -> {appId, appSKey}
 *   2. WebSocket ws://host/dispatcher/{appId}
 *   3. envia {"secret":"probe-dummy","event":[]} (secret nao-vazio basta sem cripto)
 *   4. mostra o push de estado e navega para "login"
 *
 * Objetivo: provar que o CN1 compila (passa o bytecode-compliance) e conecta no backend real.
 * Roda no simulador: ./run.sh simulator
 */
public class ShoppingCn1RemoteApp extends Lifecycle {

    private static final String BASE = "http://localhost:8080";
    private static final String WS_BASE = "ws://localhost:8080"; // sem regex/replaceFirst
    private static final String BROWSER_VSID = "7b32e816a191:0";

    private Form form;
    private SpanLabel logLabel;
    private final StringBuilder logBuf = new StringBuilder();
    private boolean navigated;

    @Override
    public void runApp() {
        form = new Form("Bridge Probe (CN1)", BoxLayout.y());
        logLabel = new SpanLabel("iniciando...");
        form.add(logLabel);
        form.show();

        new Thread(this::runProbe).start();
    }

    private void runProbe() {
        try {
            log("WebSocket.isSupported() = " + WebSocket.isSupported());

            // 1. session init (GET)
            ConnectionRequest req = new ConnectionRequest();
            req.setUrl(BASE + "/api/session/init");
            req.setPost(false);
            NetworkManager.getInstance().addToQueueAndWait(req);
            log("session/init HTTP " + req.getResponseCode());

            byte[] data = req.getResponseData();
            if (data == null) {
                log("!! sem corpo na resposta — backend no ar em " + BASE + "?");
                return;
            }
            Map<String, Object> init = JSONParser.parseJSON(new String(data));
            String appId = (String) init.get("appId");
            log("appId = " + appId);

            // 2. WebSocket
            String wsUrl = WS_BASE + "/dispatcher/" + appId;
            log("abrindo WS: " + wsUrl);

            WebSocket.build(wsUrl)
                    .onConnect(w -> {
                        log("WS conectado");
                        // 3. init message com secret dummy nao-vazio
                        w.send("{\"secret\":\"probe-dummy\",\"event\":[]}");
                    })
                    .onTextMessage((w, msg) -> {
                        log("<- " + msg);
                        // 4. apos o 1o push, navega para login (uma vez so)
                        if (!navigated) {
                            navigated = true;
                            w.send("{\"requestId\":1,\"event\":[\"" + BROWSER_VSID
                                    + ":-1\"],\"" + BROWSER_VSID + "\":{\"p.path\":\"login\"}}");
                        }
                    })
                    .onClose((w, code, reason) -> log("WS fechado: " + code + " " + reason))
                    .onError((w, ex) -> log("!! WS erro: " + ex))
                    .connect();

        } catch (Exception e) {
            log("!! excecao: " + e);
        }
    }

    private void log(String s) {
        logBuf.append(s).append("\n\n");
        CN.callSerially(() -> {
            logLabel.setText(logBuf.toString());
            form.revalidate();
        });
    }
}
