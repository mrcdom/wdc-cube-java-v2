package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.IconButton;

/** Linha do carrinho: nome | stepper (− qtd +) | subtotal | remover (×). */
public class CartItemCn1View extends AbstractItemCn1View<Object> {

    private static final CartSel sel = CartSel.INSTANCE;

    // Lado (mm) dos botões — densidade-independente (ver util.Px).
    private static final float STEP_BTN_MM = 7f;
    private static final float REMOVE_BTN_MM = 7f;

    private final BiConsumer<Long, Integer> onModify;
    private final Consumer<Long> onRemove;
    private Consumer<String> name;
    private Consumer<String> qty;
    private Consumer<String> subtotal;
    private long currentId = -1;
    private int currentQty = 1;

    public CartItemCn1View(BiConsumer<Long, Integer> onModify, Consumer<Long> onRemove) {
        this.onModify = onModify;
        this.onRemove = onRemove;
    }

    @Override
    protected Container build() {
        return Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.CART_ITEM_ROW);
            dom.label(BorderLayout.CENTER, l -> {
                l.setUIID(sel.CART_ITEM_NAME);
                l.setEndsWith3Points(true); // nome longo vira "…" em vez de cortar no meio
                name = Guard.text(l);
            });
            dom.boxX(BorderLayout.EAST, east -> {
                dom.boxX(stepper -> {
                    stepper.setUIID(sel.CART_STEPPER);
                    dom.add(new IconButton(sel.QTY_BTN, FontImage.MATERIAL_REMOVE, 3f, Px.mm(STEP_BTN_MM),
                            () -> onModify.accept(currentId, currentQty - 1)), null);
                    dom.label(l -> {
                        l.setUIID(sel.QTY_VALUE);
                        l.setPreferredW(Px.mm(11)); // largura fixa: a fonte negrito nativa mede menos
                        qty = Guard.text(l);         // que o glifo e o cortaria; área de conteúdo maior
                    });
                    dom.add(new IconButton(sel.QTY_BTN, FontImage.MATERIAL_ADD, 3f, Px.mm(STEP_BTN_MM),
                            () -> onModify.accept(currentId, currentQty + 1)), null);
                });
                dom.label(l -> {
                    l.setUIID(sel.CART_ITEM_SUBTOTAL);
                    subtotal = Guard.text(l);
                });
                dom.add(new IconButton(sel.CART_REMOVE_BTN, FontImage.MATERIAL_CLOSE, 3f, Px.mm(REMOVE_BTN_MM),
                        () -> onRemove.accept(currentId)), null);
            });
        });
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        currentQty = Json.intOf(m, "quantity");
        name.accept(Json.str(m, "name"));
        qty.accept(String.valueOf(currentQty));
        subtotal.accept(Money.format(Json.doubleOf(m, "price") * currentQty));
    }
}
