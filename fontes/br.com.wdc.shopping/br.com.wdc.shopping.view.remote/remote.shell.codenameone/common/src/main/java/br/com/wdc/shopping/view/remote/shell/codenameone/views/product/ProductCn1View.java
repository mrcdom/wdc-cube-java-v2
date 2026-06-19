package br.com.wdc.shopping.view.remote.shell.codenameone.views.product;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.SimpleHtml;

/** Detalhe do produto (classId {@value #CLASS_ID}). */
public class ProductCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "48b693f67410";
    private static final int EVT_BACK = 1;
    private static final int EVT_ADD_TO_CART = 2;
    private static final int DETAIL_PX = 260;

    private Label image;
    private Label name;
    private Label price;
    private Container description;
    private TextField qty;
    private long currentId = -1;
    private String lastHtml = "";

    public ProductCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        Cn1Dom.render(root, (dom, r) -> {
            image = dom.label(l -> { });
            name = dom.label(l -> { });
            price = dom.label(l -> { });
            description = dom.boxY(c -> { });
            dom.label(l -> l.setText("Quantidade:"));
            qty = dom.textField(tf -> {
                tf.setText("1");
                tf.setConstraint(TextArea.NUMERIC);
            });
            dom.button(b -> {
                b.setText("Adicionar ao carrinho");
                b.addActionListener(e -> {
                    Map<String, Object> form = new HashMap<>();
                    form.put("p.quantity", parseIntOr(qty.getText(), 1));
                    submit(EVT_ADD_TO_CART, form);
                });
            });
            dom.button(b -> {
                b.setText("Voltar");
                b.addActionListener(e -> submit(EVT_BACK));
            });
        });
        return root;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> p = Json.asMap(state().get("product"));
        long id = Json.longOf(p, "id");
        if (id != currentId) {
            currentId = id;
            image.setIcon(Images.product(id, DETAIL_PX));
        }
        name.setText(Json.str(p, "name"));
        price.setText(Money.format(Json.doubleOf(p, "price")));
        String html = Json.str(p, "description");
        if (!html.equals(lastHtml)) {
            lastHtml = html;
            SimpleHtml.render(description, html);
        }
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
