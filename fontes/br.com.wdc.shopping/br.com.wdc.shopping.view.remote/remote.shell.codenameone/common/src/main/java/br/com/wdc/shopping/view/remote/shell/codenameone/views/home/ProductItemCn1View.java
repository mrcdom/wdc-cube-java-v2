package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Item da lista de produtos: thumbnail + nome/preço, abre o detalhe ao tocar. */
public class ProductItemCn1View extends AbstractItemCn1View<Object> {

    private static final int THUMB = 90;

    private final Consumer<Long> onOpen;
    private Button button;
    private long currentId = -1;

    public ProductItemCn1View(Consumer<Long> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    protected Container build() {
        Container c = new Container(new BorderLayout());
        button = new Button();
        button.addActionListener(e -> onOpen.accept(currentId));
        c.add(BorderLayout.CENTER, button);
        return c;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        long id = Json.longOf(m, "id");
        button.setText(Json.str(m, "name") + "  —  " + Money.format(Json.doubleOf(m, "price")));
        if (id != currentId) {
            currentId = id;
            button.setIcon(Images.product(id, THUMB));
        }
    }
}
