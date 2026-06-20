package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.GridLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/** Painel/catálogo de produtos (classId {@value #CLASS_ID}) — grid responsivo de cards. */
public class ProductsPanelCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "a1b2c3d4e5f6";
    private static final int EVT_OPEN_PRODUCT = 1;

    private Container grid;
    private final List<ProductItemCn1View> items = new ArrayList<>();

    public ProductsPanelCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        grid = new Container(new GridLayout(columns()));
        grid.setUIID("ProductsPanel");
        // grid em BoxLayout.y => assume a altura preferida (linhas no topo, cards na altura do
        // conteúdo) em vez de esticar para preencher o CENTER do BorderLayout.
        Container wrapper = new Container(BoxLayout.y());
        wrapper.setScrollableY(true);
        wrapper.add(grid);
        return wrapper;
    }

    @Override
    public void doUpdate() {
        List<Object> products = Json.asList(state().get("products"));
        syncList(grid, products, items, () -> new ProductItemCn1View(this::openProduct));
    }

    private void openProduct(long productId) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.productId", productId);
        submit(EVT_OPEN_PRODUCT, form);
    }

    /** Colunas conforme a largura (2 / 3 / 4), como o grid responsivo do React. */
    private static int columns() {
        int w = Display.getInstance().getDisplayWidth();
        if (w >= 1600) {
            return 4;
        }
        if (w >= 1100) {
            return 3;
        }
        return 2;
    }
}
