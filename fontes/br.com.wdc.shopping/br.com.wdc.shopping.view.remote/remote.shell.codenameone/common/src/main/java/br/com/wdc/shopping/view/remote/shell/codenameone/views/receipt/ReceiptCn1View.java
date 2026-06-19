package br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt;

import java.util.ArrayList;
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

/** Recibo da compra (classId {@value #CLASS_ID}). */
public class ReceiptCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "e8d0bd8ae3bc";
    private static final int EVT_BACK = 1;

    private Label total;
    private Container list;
    private final List<ReceiptItemCn1View> items = new ArrayList<>();

    public ReceiptCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        root.setScrollableY(true);
        root.setUIID("Card");
        Cn1Dom.render(root, (dom, r) -> {
            dom.label(l -> {
                l.setText("Compra realizada com sucesso!");
                l.setUIID("SectionTitle");
            });
            total = dom.label(l -> l.setUIID("Price"));
            list = dom.boxY(l -> { });
            dom.button(b -> {
                b.setText("Continuar comprando");
                b.setUIID("LinkButton");
                b.addActionListener(e -> submit(EVT_BACK));
            });
        });
        return root;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> receipt = Json.asMap(state().get("receipt"));
        total.setText("Total: " + Money.format(Json.doubleOf(receipt, "total")));
        List<Object> receiptItems = Json.asList(receipt != null ? receipt.get("items") : null);
        syncList(list, receiptItems, items, ReceiptItemCn1View::new);
    }
}
