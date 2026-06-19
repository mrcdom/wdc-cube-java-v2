package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Form;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

/**
 * Shell fino Codename One do padrão remote-shell.
 *
 * <p>
 * Fase 1 — render: conecta no bridge ({@link BridgeSession}) e renderiza a tela corrente a partir
 * dos ViewStates recebidos. A primeira tela real é o <b>login</b> (classId {@code c677cda52d14}).
 * Telas ainda não mapeadas caem num render de depuração que mostra o estado cru.
 * </p>
 *
 * <p>
 * O botão "Entrar" é placeholder: o login seguro exige o handshake de criptografia (cifra da senha),
 * que entra na próxima fase. Navegação e render já funcionam sem cripto.
 * </p>
 */
public class ShoppingCn1RemoteApp extends Lifecycle {

    /** classIds das telas (prefixo do vsid no protocolo bridge). */
    private static final String LOGIN_CLASS_ID = "c677cda52d14";
    private static final String HOME_CLASS_ID = "473dbdd7a36a";

    /** Event codes (espelham os skeletons dos presenters). */
    private static final int HOME_EVT_LOGOUT = 1;
    private static final int HOME_EVT_OPEN_CART = 2;
    private static final int PRODUCTS_EVT_OPEN_PRODUCT = 1;

    private BridgeSession session;

    @Override
    public void runApp() {
        showStatus("Conectando ao servidor...");

        session = new BridgeSession("http://localhost:8080", s -> render());
        new Thread(() -> {
            try {
                session.connect();
            } catch (Exception e) {
                CN.callSerially(() -> showStatus("Falha ao conectar: " + e.getMessage()));
            }
        }).start();
    }

    // :: Render dispatch

    private void render() {
        String vsid = session.currentScreenVsid();
        String classId = BridgeSession.classIdOf(vsid);

        Form form;
        if (LOGIN_CLASS_ID.equals(classId)) {
            form = renderLogin(vsid, session.state(vsid));
        } else if (HOME_CLASS_ID.equals(classId)) {
            form = renderHome(vsid, session.state(vsid));
        } else {
            form = renderDebug(vsid, classId);
        }
        form.show();
    }

    // :: Login

    private Form renderLogin(String vsid, Map<String, Object> st) {
        Form f = new Form("WDC Shopping", BoxLayout.y());

        f.add(new Label("Entrar"));

        TextField user = new TextField();
        user.setHint("Usuário");
        TextField pass = new TextField();
        pass.setHint("Senha");
        pass.setConstraint(TextArea.PASSWORD);
        f.add(user);
        f.add(pass);

        boolean loading = boolOf(st, "loading");
        int errorCode = intOf(st, "errorCode");
        if (loading) {
            f.add(new Label("Entrando..."));
        }
        if (errorCode != 0) {
            Label err = new Label("Falha no login (código " + errorCode + ")");
            err.getAllStyles().setFgColor(0xcc0000);
            f.add(err);
        }

        Button login = new Button("Entrar");
        login.addActionListener(e -> {
            Map<String, Object> form = new HashMap<>();
            form.put("p.userName", user.getText());
            form.put("p.password", session.cipher(pass.getText()));
            session.submit(vsid, 1, form);
        });
        f.add(login);

        return f;
    }

    // :: Home

    private Form renderHome(String vsid, Map<String, Object> st) {
        Form f = new Form("WDC Shopping", BoxLayout.y());

        String nick = strOf(st, "nickName");
        int cart = intOf(st, "cartItemCount");
        f.add(new Label("Olá, " + nick));

        Container actions = new Container(BoxLayout.x());
        Button cartBtn = new Button("Carrinho (" + cart + ")");
        cartBtn.addActionListener(e -> session.submit(vsid, HOME_EVT_OPEN_CART, new HashMap<>()));
        Button logout = new Button("Sair");
        logout.addActionListener(e -> session.submit(vsid, HOME_EVT_LOGOUT, new HashMap<>()));
        actions.add(cartBtn);
        actions.add(logout);
        f.add(actions);

        f.add(new Label("Produtos"));
        String panelVsid = strOf(st, "productsPanelViewId");
        Map<String, Object> panel = session.state(panelVsid);
        Object productsObj = panel != null ? panel.get("products") : null;
        if (productsObj instanceof List) {
            for (Object o : (List<?>) productsObj) {
                if (!(o instanceof Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> p = (Map<String, Object>) o;
                final long productId = (long) intOf(p, "id");
                String label = strOf(p, "name") + "  —  R$ " + strOf(p, "price");
                Button row = new Button(label);
                row.addActionListener(e -> {
                    Map<String, Object> form = new HashMap<>();
                    form.put("p.productId", productId);
                    session.submit(panelVsid, PRODUCTS_EVT_OPEN_PRODUCT, form);
                });
                f.add(row);
            }
        }
        return f;
    }

    // :: Fallback de depuração (telas ainda não mapeadas)

    private Form renderDebug(String vsid, String classId) {
        Form f = new Form("Bridge (debug)", BoxLayout.y());
        f.add(new Label("uri: " + session.uri()));
        f.add(new Label("tela: " + classId + "  (" + vsid + ")"));
        SpanLabel dump = new SpanLabel(dumpStates());
        f.add(dump);
        return f;
    }

    private String dumpStates() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, Object>> e : session.allStates().entrySet()) {
            sb.append(e.getKey()).append(" -> ").append(e.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    // :: util

    private void showStatus(String text) {
        Form f = new Form("WDC Shopping", BoxLayout.y());
        f.add(new SpanLabel(text));
        f.show();
    }

    private static String strOf(Map<String, Object> st, String key) {
        Object o = st != null ? st.get(key) : null;
        return o != null ? o.toString() : "";
    }

    private static boolean boolOf(Map<String, Object> st, String key) {
        Object o = st != null ? st.get(key) : null;
        if (o instanceof Boolean) {
            return (Boolean) o;
        }
        return o != null && "true".equals(o.toString());
    }

    private static int intOf(Map<String, Object> st, String key) {
        Object o = st != null ? st.get(key) : null;
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return o != null ? Integer.parseInt(o.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
