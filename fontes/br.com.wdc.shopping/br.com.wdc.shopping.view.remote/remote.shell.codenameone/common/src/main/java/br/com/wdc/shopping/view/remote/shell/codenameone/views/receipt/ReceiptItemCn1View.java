package br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt;

import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Linha do recibo (cupom): descrição | quantidade | valor, alinhada com o cabeçalho da tabela. */
public class ReceiptItemCn1View extends AbstractItemCn1View<Object> {

    private Label desc;
    private Label qty;
    private Label value;

    @Override
    protected Container build() {
        Container root = new Container(new BorderLayout());
        root.setUIID("ReceiptItemRow");
        Cn1Dom.render(root, (dom, r) -> {
            desc = dom.label(BorderLayout.CENTER, l -> l.setUIID("ReceiptItemDesc"));
            ReceiptCn1View.mono(desc);
            dom.boxX(BorderLayout.EAST, cols -> {
                qty = dom.label(l -> l.setUIID("ReceiptItemQty"));
                qty.setPreferredW(ReceiptCn1View.COL_QTY_W);
                ReceiptCn1View.mono(qty);
                value = dom.label(l -> l.setUIID("ReceiptItemValue"));
                value.setPreferredW(ReceiptCn1View.COL_VALUE_W);
                ReceiptCn1View.mono(value);
            });
        });
        return root;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        desc.setText(Json.str(m, "description"));
        qty.setText(String.valueOf(Json.intOf(m, "quantity")));
        value.setText(Money.format(Json.doubleOf(m, "value")));
    }
}
