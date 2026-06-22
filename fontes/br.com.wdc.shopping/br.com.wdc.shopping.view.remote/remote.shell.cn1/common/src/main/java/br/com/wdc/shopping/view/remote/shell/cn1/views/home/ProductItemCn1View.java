package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Clickable;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Images;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;

/** Card de produto: imagem (fundo gradiente) + nome + preço; o card inteiro abre o detalhe. */
public class ProductItemCn1View extends AbstractItemCn1View<Object> {

    private static final HomeSel sel = HomeSel.INSTANCE;

    // Dimensões em mm (densidade-independente). Em px fixos o card fica minúsculo em telas de alta
    // densidade e o nome/preço (fonte escalada por densidade) não cabem. O FlowLayout do painel quebra
    // a linha quando não couber; valores tunáveis (ex.: ~2 cards por linha no telefone).
    private static final float IMG_MM = 17f;
    private static final float CARD_W_MM = 26f;
    private static final float CARD_H_MM = 38f;

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
        Container content = Cn1Dom.render(BoxLayout.y(), (dom, c) -> {
            // imagem centralizada num wrap (FlowLayout mantém o tamanho quadrado, sem distorcer)
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                wrap.setUIID(sel.PRODUCT_CARD_IMAGE);
                image = dom.label(l -> { });
            });
            name = dom.label(l -> {
                l.setUIID(sel.PRODUCT_CARD_NAME);
                l.setEndsWith3Points(true); // nome longo vira "…" em vez de cortar no meio
            });
            price = dom.label(l -> l.setUIID(sel.PRODUCT_CARD_PRICE));
        });
        Container card = Clickable.card(sel.PRODUCT_CARD, content, () -> onOpen.accept(currentId));
        card.setPreferredSize(new Dimension(Px.mm(CARD_W_MM), Px.mm(CARD_H_MM)));
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
            image.setIcon(Images.product(id, Px.mm(IMG_MM)));
        }
    }
}
