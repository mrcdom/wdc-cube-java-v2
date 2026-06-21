package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;

/** Linha do recibo (cupom): descrição | quantidade | valor, alinhada com o cabeçalho da tabela. */
public class ReceiptItemCn1View extends AbstractItemCn1View<Object> {

    private static final ReceiptSel sel = ReceiptSel.INSTANCE;

    private Label desc;
    private Label qty;
    private Label value;

    @Override
    protected Container build() {
        return Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.RECEIPT_ITEM_ROW);
            desc = dom.label(BorderLayout.CENTER, l -> l.setUIID(sel.RECEIPT_ITEM_DESC));
            ReceiptCn1View.mono(desc);
            dom.boxX(BorderLayout.EAST, cols -> {
                qty = dom.label(l -> l.setUIID(sel.RECEIPT_ITEM_QTY));
                qty.setPreferredW(ReceiptCn1View.COL_QTY_W);
                ReceiptCn1View.mono(qty);
                value = dom.label(l -> l.setUIID(sel.RECEIPT_ITEM_VALUE));
                value.setPreferredW(ReceiptCn1View.COL_VALUE_W);
                ReceiptCn1View.mono(value);
            });
        });
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        desc.setText(Json.str(m, "description"));
        qty.setText(String.valueOf(Json.intOf(m, "quantity")));
        value.setText(Money.format(Json.doubleOf(m, "value")));
    }
}
