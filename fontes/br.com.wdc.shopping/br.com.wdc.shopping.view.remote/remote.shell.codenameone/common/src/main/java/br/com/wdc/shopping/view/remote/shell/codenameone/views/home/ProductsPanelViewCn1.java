package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/** Painel/catálogo de produtos (classId {@value #CLASS_ID}). */
public class ProductsPanelViewCn1 extends AbstractViewCn1 {

    public static final String CLASS_ID = "a1b2c3d4e5f6";
    private static final int EVT_OPEN_PRODUCT = 1;

    private Container list;
    private final List<ProductItemViewCn1> items = new ArrayList<>();

    public ProductsPanelViewCn1(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        list = new Container(BoxLayout.y());
        list.setScrollableY(true);
        return list;
    }

    @Override
    public void doUpdate() {
        List<Object> products = Json.asList(state().get("products"));
        syncList(list, products, items, () -> new ProductItemViewCn1(this::openProduct));
    }

    private void openProduct(long productId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        submit(EVT_OPEN_PRODUCT, form);
    }
}
