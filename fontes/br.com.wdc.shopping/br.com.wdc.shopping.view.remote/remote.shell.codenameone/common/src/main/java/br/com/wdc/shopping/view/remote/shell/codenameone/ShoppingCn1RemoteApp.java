package br.com.wdc.shopping.view.remote.shell.codenameone;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.components.SpanLabel;
import com.codename1.system.Lifecycle;
import com.codename1.ui.CN;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.EncodedImage;
import com.codename1.ui.Form;
import com.codename1.ui.Image;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.URLImage;
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

    private static final String BASE = "http://localhost:8080";
    private static final int THUMB_PX = 90;
    private static final int DETAIL_PX = 260;

    /** classIds das telas (prefixo do vsid no protocolo bridge). */
    private static final String LOGIN_CLASS_ID = "c677cda52d14";
    private static final String HOME_CLASS_ID = "473dbdd7a36a";
    private static final String PRODUCT_CLASS_ID = "48b693f67410";
    private static final String CART_CLASS_ID = "7eb485e5f843";
    private static final String RECEIPT_CLASS_ID = "e8d0bd8ae3bc";

    /** Event codes (espelham os skeletons dos presenters). */
    private static final int HOME_EVT_LOGOUT = 1;
    private static final int HOME_EVT_OPEN_CART = 2;
    private static final int PRODUCTS_EVT_OPEN_PRODUCT = 1;
    private static final int PRODUCT_EVT_BACK = 1;
    private static final int PRODUCT_EVT_ADD_TO_CART = 2;
    private static final int CART_EVT_BUY = 1;
    private static final int CART_EVT_REMOVE = 2;
    private static final int CART_EVT_BACK = 3;
    private static final int RECEIPT_EVT_BACK = 1;

    private BridgeSession session;

    @Override
    public void runApp() {
        showStatus("Conectando ao servidor...");

        session = new BridgeSession(BASE, s -> render());
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
        } else if (PRODUCT_CLASS_ID.equals(classId)) {
            form = renderProduct(vsid, session.state(vsid));
        } else if (CART_CLASS_ID.equals(classId)) {
            form = renderCart(vsid, session.state(vsid));
        } else if (RECEIPT_CLASS_ID.equals(classId)) {
            form = renderReceipt(vsid, session.state(vsid));
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
                Button row = new Button(label, productImage(productId, THUMB_PX));
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

    // :: Produto

    private Form renderProduct(String vsid, Map<String, Object> st) {
        Form f = new Form("Produto", BoxLayout.y());
        Map<String, Object> product = asMap(st != null ? st.get("product") : null);

        f.add(new Label(productImage((long) intOf(product, "id"), DETAIL_PX)));
        f.add(new Label(strOf(product, "name")));
        f.add(new Label("R$ " + strOf(product, "price")));
        f.add(new SpanLabel(strOf(product, "description")));

        f.add(new Label("Quantidade:"));
        TextField qty = new TextField("1");
        qty.setConstraint(TextArea.NUMERIC);
        f.add(qty);

        Button add = new Button("Adicionar ao carrinho");
        add.addActionListener(e -> {
            Map<String, Object> form = new HashMap<>();
            form.put("p.quantity", parseIntOr(qty.getText(), 1));
            session.submit(vsid, PRODUCT_EVT_ADD_TO_CART, form);
        });
        Button back = new Button("Voltar");
        back.addActionListener(e -> session.submit(vsid, PRODUCT_EVT_BACK, new HashMap<>()));
        f.add(add);
        f.add(back);
        return f;
    }

    // :: Carrinho

    private Form renderCart(String vsid, Map<String, Object> st) {
        Form f = new Form("Carrinho", BoxLayout.y());

        double total = 0;
        Object itemsObj = st != null ? st.get("items") : null;
        if (itemsObj instanceof List) {
            for (Object o : (List<?>) itemsObj) {
                Map<String, Object> it = asMap(o);
                if (it == null) {
                    continue;
                }
                final long productId = (long) intOf(it, "id");
                int quantity = intOf(it, "quantity");
                total += doubleOf(it, "price") * quantity;

                Container row = new Container(BoxLayout.x());
                row.add(new Label(strOf(it, "name") + "  x" + quantity + "  R$ " + strOf(it, "price")));
                Button remove = new Button("Remover");
                remove.addActionListener(e -> {
                    Map<String, Object> form = new HashMap<>();
                    form.put("p.productId", productId);
                    session.submit(vsid, CART_EVT_REMOVE, form);
                });
                row.add(remove);
                f.add(row);
            }
        }

        f.add(new Label("Total: R$ " + money(total)));
        Button buy = new Button("Comprar");
        buy.addActionListener(e -> session.submit(vsid, CART_EVT_BUY, new HashMap<>()));
        Button cont = new Button("Continuar comprando");
        cont.addActionListener(e -> session.submit(vsid, CART_EVT_BACK, new HashMap<>()));
        f.add(buy);
        f.add(cont);
        return f;
    }

    // :: Recibo

    private Form renderReceipt(String vsid, Map<String, Object> st) {
        Form f = new Form("Recibo", BoxLayout.y());
        f.add(new Label("Compra realizada com sucesso!"));

        Map<String, Object> receipt = asMap(st != null ? st.get("receipt") : null);
        if (receipt != null) {
            f.add(new Label("Total: R$ " + strOf(receipt, "total")));
            Object items = receipt.get("items");
            if (items instanceof List) {
                for (Object o : (List<?>) items) {
                    Map<String, Object> it = asMap(o);
                    if (it == null) {
                        continue;
                    }
                    f.add(new Label(strOf(it, "description") + "  x" + intOf(it, "quantity")
                            + "  R$ " + strOf(it, "value")));
                }
            }
        }

        Button back = new Button("Continuar comprando");
        back.addActionListener(e -> session.submit(vsid, RECEIPT_EVT_BACK, new HashMap<>()));
        f.add(back);
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map ? (Map<String, Object>) o : null;
    }

    private static double doubleOf(Map<String, Object> st, String key) {
        Object o = st != null ? st.get(key) : null;
        if (o instanceof Number) {
            return ((Number) o).doubleValue();
        }
        try {
            return o != null ? Double.parseDouble(o.toString()) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    /** Formata um valor monetário com 2 casas (evita lixo de ponto flutuante). */
    private static String money(double v) {
        long cents = Math.round(v * 100);
        long whole = cents / 100;
        long frac = Math.abs(cents % 100);
        return whole + "." + (frac < 10 ? "0" + frac : "" + frac);
    }

    /**
     * Imagem do produto via {@code /image/product/{id}.png} — download assíncrono com cache em
     * Storage ({@link URLImage}); mostra um placeholder até carregar e redimensiona para {@code size}.
     */
    private static Image productImage(long id, int size) {
        EncodedImage placeholder = EncodedImage.createFromImage(Image.createImage(size, size, 0xffeeeeee), false);
        String url = BASE + "/image/product/" + id + ".png";
        String storage = "prod_" + id + "_" + size + ".png";
        return URLImage.createToStorage(placeholder, storage, url, URLImage.RESIZE_SCALE_TO_FILL);
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
