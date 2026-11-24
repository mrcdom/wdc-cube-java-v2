package br.com.wdc.store.view.remote.shell.cn1.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.store.view.remote.shell.cn1.util.Clickable;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Images;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;
import br.com.wdc.store.view.remote.shell.cn1.util.Money;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;

/** Product card: image (gradient background) + name + price; the whole card opens the detail. */
public class ProductItemCn1View extends AbstractItemCn1View<Object> {

    private static final HomeSel sel = HomeSel.INSTANCE;

    // Dimensions in mm (density-independent). In fixed px the card gets tiny on high-density screens
    // and the name/price (density-scaled font) do not fit. The panel's FlowLayout wraps the line when
    // it does not fit; tunable values (e.g.: ~2 cards per row on a phone).
    private static final float IMG_MM = 17f;
    private static final float CARD_W_MM = 26f;
    private static final float CARD_H_MM = 38f;

    private final Consumer<Long> onOpen;
    private Label image;
    private Consumer<String> name;
    private Consumer<String> price;
    private long currentId = -1;

    public ProductItemCn1View(Consumer<Long> onOpen) {
        this.onOpen = onOpen;
    }

    @Override
    protected Container build() {
        Container content = Cn1Dom.render(BoxLayout.y(), (dom, c) -> {
            // image centered in a wrap (FlowLayout keeps the square size, without distortion)
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                wrap.setUIID(sel.PRODUCT_CARD_IMAGE);
                image = dom.label(l -> { });
            });
            dom.label(l -> {
                l.setUIID(sel.PRODUCT_CARD_NAME);
                l.setEndsWith3Points(true); // a long name becomes "…" instead of being cut in the middle

                name = Guard.text(l);
            });
            dom.label(l -> {
                l.setUIID(sel.PRODUCT_CARD_PRICE);
                price = Guard.text(l);
            });
        });
        Container card = Clickable.card(sel.PRODUCT_CARD, content, () -> onOpen.accept(currentId));
        card.setPreferredSize(new Dimension(Px.mm(CARD_W_MM), Px.mm(CARD_H_MM)));
        return card;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        long id = Json.longOf(m, "id");
        name.accept(Json.str(m, "name"));
        price.accept(Money.format(Json.doubleOf(m, "price")));
        if (id != currentId) {
            currentId = id;
            image.setIcon(Images.product(id, Px.mm(IMG_MM)));
        }
    }
}
