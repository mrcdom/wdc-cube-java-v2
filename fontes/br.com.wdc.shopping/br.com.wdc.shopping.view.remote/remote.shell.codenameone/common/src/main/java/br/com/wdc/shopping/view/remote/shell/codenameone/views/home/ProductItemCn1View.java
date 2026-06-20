package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Clickable;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Images;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Money;

/** Card de produto: imagem (fundo gradiente) + nome + preço; o card inteiro abre o detalhe. */
public class ProductItemCn1View extends AbstractItemCn1View<Object> {

    private static final int IMG = 190;
    /** Tamanho fixo do card — o FlowLayout do painel quebra a linha quando não couber. */
    private static final int CARD_W = 300;
    private static final int CARD_H = 410;

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
        Container content = new Container(BoxLayout.y());
        Cn1Dom.render(content, (dom, c) -> {
            // imagem centralizada num wrap (FlowLayout mantém o tamanho quadrado, sem distorcer)
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                wrap.setUIID("ProductCardImage");
                image = dom.label(l -> { });
            });
            name = dom.label(l -> l.setUIID("ProductCardName"));
            price = dom.label(l -> l.setUIID("ProductCardPrice"));
        });
        Container card = Clickable.card("ProductCard", content, () -> onOpen.accept(currentId));
        card.setPreferredSize(new Dimension(CARD_W, CARD_H));
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
