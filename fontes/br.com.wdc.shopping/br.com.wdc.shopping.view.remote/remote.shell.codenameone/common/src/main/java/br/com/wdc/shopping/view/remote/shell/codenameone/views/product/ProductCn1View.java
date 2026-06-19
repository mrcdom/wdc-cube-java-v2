package br.com.wdc.shopping.view.remote.shell.codenameone.views.product;

import java.util.HashMap;
import java.util.Map;

import com.codename1.components.SpanLabel;
import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Detalhe do produto (classId {@value #CLASS_ID}). */
public class ProductCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "48b693f67410";
    private static final int EVT_BACK = 1;
    private static final int EVT_ADD_TO_CART = 2;
    private static final int DETAIL_PX = 260;

    private Label image;
    private Label name;
    private Label price;
    private SpanLabel description;
    private TextField qty;
    private long currentId = -1;

    public ProductCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container c = new Container(BoxLayout.y());
        c.setScrollableY(true);

        image = new Label();
        name = new Label("");
        price = new Label("");
        description = new SpanLabel("");
        qty = new TextField("1");
        qty.setConstraint(TextArea.NUMERIC);

        Button add = new Button("Adicionar ao carrinho");
        add.addActionListener(e -> {
            Map<String, Object> form = new HashMap<>();
            form.put("p.quantity", parseIntOr(qty.getText(), 1));
            submit(EVT_ADD_TO_CART, form);
        });
        Button back = new Button("Voltar");
        back.addActionListener(e -> submit(EVT_BACK));

        c.add(image);
        c.add(name);
        c.add(price);
        c.add(description);
        c.add(new Label("Quantidade:"));
        c.add(qty);
        c.add(add);
        c.add(back);
        return c;
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
        description.setText(Json.str(p, "description"));
    }

    private static int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}
