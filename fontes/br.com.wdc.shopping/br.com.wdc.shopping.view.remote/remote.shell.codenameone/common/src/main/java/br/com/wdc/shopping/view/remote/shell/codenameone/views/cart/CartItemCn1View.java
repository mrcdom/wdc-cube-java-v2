package br.com.wdc.shopping.view.remote.shell.codenameone.views.cart;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Item do carrinho: nome ×quantidade + preço, com botão de remover. */
public class CartItemCn1View extends AbstractItemCn1View<Object> {

    private final Consumer<Long> onRemove;
    private Label label;
    private long currentId = -1;

    public CartItemCn1View(Consumer<Long> onRemove) {
        this.onRemove = onRemove;
    }

    @Override
    protected Container build() {
        Container c = new Container(new BorderLayout());
        label = new Label("");
        Button remove = new Button("Remover");
        remove.addActionListener(e -> onRemove.accept(currentId));
        c.add(BorderLayout.CENTER, label);
        c.add(BorderLayout.EAST, remove);
        return c;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        label.setText(Json.str(m, "name") + "  x" + Json.intOf(m, "quantity")
                + "  " + Money.format(Json.doubleOf(m, "price")));
    }
}
