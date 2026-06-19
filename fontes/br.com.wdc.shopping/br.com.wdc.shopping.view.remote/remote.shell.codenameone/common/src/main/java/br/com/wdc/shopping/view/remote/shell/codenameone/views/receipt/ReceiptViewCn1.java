package br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Recibo da compra (classId {@value #CLASS_ID}). */
public class ReceiptViewCn1 extends AbstractViewCn1 {

    public static final String CLASS_ID = "e8d0bd8ae3bc";
    private static final int EVT_BACK = 1;

    private Label total;
    private Container list;
    private final List<ReceiptItemViewCn1> items = new ArrayList<>();

    public ReceiptViewCn1(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container c = new Container(BoxLayout.y());
        c.setScrollableY(true);
        c.add(new Label("Compra realizada com sucesso!"));
        total = new Label("");
        list = new Container(BoxLayout.y());

        Button back = new Button("Continuar comprando");
        back.addActionListener(e -> submit(EVT_BACK));

        c.add(total);
        c.add(list);
        c.add(back);
        return c;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> receipt = Json.asMap(state().get("receipt"));
        total.setText("Total: " + Money.format(Json.doubleOf(receipt, "total")));
        List<Object> receiptItems = Json.asList(receipt != null ? receipt.get("items") : null);
        syncList(list, receiptItems, items, ReceiptItemViewCn1::new);
    }
}
