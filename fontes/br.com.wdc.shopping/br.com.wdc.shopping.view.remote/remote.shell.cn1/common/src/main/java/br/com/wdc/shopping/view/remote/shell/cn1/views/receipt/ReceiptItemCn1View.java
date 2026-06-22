package br.com.wdc.shopping.view.remote.shell.cn1.views.receipt;

import java.util.Map;

import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;

/** Linha do recibo (cupom): descrição | quantidade | valor, alinhada com o cabeçalho da tabela. */
public class ReceiptItemCn1View extends AbstractItemCn1View<Object> {

    private static final ReceiptSel sel = ReceiptSel.INSTANCE;

    private Consumer<String> desc;
    private Consumer<String> qty;
    private Consumer<String> value;

    @Override
    protected Container build() {
        return Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.RECEIPT_ITEM_ROW);
            dom.label(BorderLayout.CENTER, l -> {
                l.setUIID(sel.RECEIPT_ITEM_DESC);
                ReceiptCn1View.mono(l);
                desc = Guard.text(l);
            });
            dom.boxX(BorderLayout.EAST, cols -> {
                dom.label(l -> {
                    l.setUIID(sel.RECEIPT_ITEM_QTY);
                    l.setPreferredW(Px.mm(ReceiptCn1View.COL_QTY_W_MM));
                    ReceiptCn1View.mono(l);
                    qty = Guard.text(l);
                });
                dom.label(l -> {
                    l.setUIID(sel.RECEIPT_ITEM_VALUE);
                    l.setPreferredW(Px.mm(ReceiptCn1View.COL_VALUE_W_MM));
                    ReceiptCn1View.mono(l);
                    value = Guard.text(l);
                });
            });
        });
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        desc.accept(Json.str(m, "description"));
        qty.accept(String.valueOf(Json.intOf(m, "quantity")));
        value.accept(Money.format(Json.doubleOf(m, "value")));
    }
}
