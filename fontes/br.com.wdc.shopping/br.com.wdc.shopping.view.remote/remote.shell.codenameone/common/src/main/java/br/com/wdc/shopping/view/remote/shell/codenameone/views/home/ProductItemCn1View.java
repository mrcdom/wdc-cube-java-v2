package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Card de produto: imagem (fundo gradiente) + nome + preço; o card inteiro abre o detalhe. */
public class ProductItemCn1View extends AbstractItemCn1View<Object> {

    private static final int IMG = 240;

    private final Consumer<Long> onOpen;
    private Label image;
    private Label name;
    private Label price;
    private long currentId = -1;

    public ProductItemCn1View(Consumer<Long> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    protected Container build() {
        Container card = new Container(new BorderLayout());
        card.setUIID("ProductCard");
        Cn1Dom.render(card, (dom, c) -> {
            image = dom.label(BorderLayout.NORTH, l -> l.setUIID("ProductCardImage"));
            dom.boxY(BorderLayout.CENTER, body -> {
                name = dom.label(l -> l.setUIID("ProductCardName"));
                price = dom.label(l -> l.setUIID("ProductCardPrice"));
            });
        });
        card.addPointerReleasedListener(e -> onOpen.accept(currentId));
        return card;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        long id = Json.longOf(m, "id");
        name.setText(Json.str(m, "name"));
        price.setText(Money.format(Json.doubleOf(m, "price")));
        if (id != currentId) {
            currentId = id;
            image.setIcon(Images.product(id, IMG));
        }
    }
}
