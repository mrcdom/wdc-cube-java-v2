package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Widgets;

/** Linha do carrinho: nome | stepper (− qtd +) | subtotal | remover (×). */
public class CartItemCn1View extends AbstractItemCn1View<Object> {

    private static final CartSel sel = CartSel.INSTANCE;

    private static final int STEP_BTN = 50;
    private static final int REMOVE_BTN = 50;

    private final BiConsumer<Long, Integer> onModify;
    private final Consumer<Long> onRemove;
    private Label name;
    private Label qty;
    private Label subtotal;
    private long currentId = -1;
    private int currentQty = 1;

    public CartItemCn1View(BiConsumer<Long, Integer> onModify, Consumer<Long> onRemove) {
        this.onModify = onModify;
        this.onRemove = onRemove;
    }

    @Override
    protected Container build() {
        Container root = new Container(new BorderLayout());
        root.setUIID(sel.CART_ITEM_ROW);
        Cn1Dom.render(root, (dom, r) -> {
            name = dom.label(BorderLayout.CENTER, l -> l.setUIID(sel.CART_ITEM_NAME));
            dom.boxX(BorderLayout.EAST, east -> {
                dom.boxX(stepper -> {
                    stepper.setUIID(sel.CART_STEPPER);
                    dom.add(Widgets.iconButton(sel.QTY_BTN, FontImage.MATERIAL_REMOVE, 3f, STEP_BTN,
                            () -> onModify.accept(currentId, currentQty - 1)), null);
                    qty = dom.label(l -> l.setUIID(sel.QTY_VALUE));
                    dom.add(Widgets.iconButton(sel.QTY_BTN, FontImage.MATERIAL_ADD, 3f, STEP_BTN,
                            () -> onModify.accept(currentId, currentQty + 1)), null);
                });
                subtotal = dom.label(l -> l.setUIID(sel.CART_ITEM_SUBTOTAL));
                dom.add(Widgets.iconButton(sel.CART_REMOVE_BTN, FontImage.MATERIAL_CLOSE, 3f, REMOVE_BTN,
                        () -> onRemove.accept(currentId)), null);
            });
        });
        return root;
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        currentQty = Json.intOf(m, "quantity");
        name.setText(Json.str(m, "name"));
        qty.setText(String.valueOf(currentQty));
        subtotal.setText(Money.format(Json.doubleOf(m, "price") * currentQty));
    }
}
