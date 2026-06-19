package br.com.wdc.shopping.view.remote.shell.codenameone.views.cart;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Carrinho (classId {@value #CLASS_ID}). */
public class CartCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "7eb485e5f843";
    private static final int EVT_BUY = 1;
    private static final int EVT_REMOVE = 2;
    private static final int EVT_BACK = 3;

    private Container list;
    private Label total;
    private final List<CartItemCn1View> items = new ArrayList<>();

    public CartCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        Cn1Dom.render(root, (dom, r) -> {
            list = dom.boxY(l -> { });
            total = dom.label(l -> { });
            dom.button(b -> {
                b.setText("Comprar");
                b.addActionListener(e -> submit(EVT_BUY));
            });
            dom.button(b -> {
                b.setText("Continuar comprando");
                b.addActionListener(e -> submit(EVT_BACK));
            });
        });
        return root;
    }

    @Override
    public void doUpdate() {
        List<Object> cartItems = Json.asList(state().get("items"));
        syncList(list, cartItems, items, () -> new CartItemCn1View(this::removeProduct));

        double sum = 0;
        for (Object o : cartItems) {
            Map<String, Object> m = Json.asMap(o);
            sum += Json.doubleOf(m, "price") * Json.intOf(m, "quantity");
        }
        total.setText("Total: " + Money.format(sum));
    }

    private void removeProduct(long productId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        submit(EVT_REMOVE, form);
    }
}
