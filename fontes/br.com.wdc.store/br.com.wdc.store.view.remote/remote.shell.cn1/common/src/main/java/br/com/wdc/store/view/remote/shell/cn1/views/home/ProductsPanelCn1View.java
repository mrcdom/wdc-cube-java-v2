package br.com.wdc.store.view.remote.shell.cn1.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.store.view.remote.shell.cn1.StoreCn1RemoteApp;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.store.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;

/**
 * Product panel/catalog (classId {@value #CLASS_ID}) — fluid card grid: each card has a fixed
 * width and the {@link FlowLayout} wraps to the next line when there is no room, fitting the
 * number of columns to the available width.

 */
public class ProductsPanelCn1View extends AbstractCn1View {

    private static final HomeSel sel = HomeSel.INSTANCE;

    public static final String CLASS_ID = "a1b2c3d4e5f6";
    private static final int EVT_OPEN_PRODUCT = 1;

    private Container grid;
    private final List<ProductItemCn1View> items = new ArrayList<>();

    public ProductsPanelCn1View(String vsid, BridgeSession session, StoreCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        grid = new Container(new FlowLayout(Component.LEFT, Component.TOP));
        grid.setUIID(sel.PRODUCTS_PANEL);
        grid.setScrollableY(true);
        return grid;
    }

    @Override
    public void doUpdate() {
        List<Object> products = Json.asList(scope().get("products"));
        syncList(grid, products, items, () -> new ProductItemCn1View(this::openProduct));
    }

    private void openProduct(long productId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        submit(EVT_OPEN_PRODUCT, form);
    }
}
