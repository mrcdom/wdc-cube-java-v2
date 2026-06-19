package br.com.wdc.shopping.view.remote.shell.codenameone.views.receipt;

import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Item do recibo: descrição ×quantidade + valor. */
public class ReceiptItemCn1View extends AbstractItemCn1View<Object> {

    private Label label;

    @Override
    protected Container build() {
        Container c = new Container(new BorderLayout());
        label = new Label("");
        c.add(BorderLayout.CENTER, label);
        return c;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        label.setText(Json.str(m, "description") + "  x" + Json.intOf(m, "quantity")
                + "  " + Money.format(Json.doubleOf(m, "value")));
    }
}
