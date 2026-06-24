package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.IconButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.QtyStepper;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Linha do carrinho: nome | stepper (− qtd +) | subtotal | remover (×).
 *
 * <p>
 * As peças são construídas <b>uma vez</b> e o {@code doUpdate()} as <b>arranja</b> conforme o modo,
 * re-montando só na troca (reusa as peças). No <b>expandido</b> tudo fica numa linha (nome à esquerda,
 * controles à direita); no <b>compacto</b> os controles não cabem ao lado do nome — o card estreito
 * espremeria o nome a zero e empurraria o × para fora da borda — então empilha em duas linhas:
 * nome | subtotal em cima, stepper | × embaixo.
 * </p>
 */
public class CartItemCn1View extends AbstractItemCn1View<Object> {

    private static final CartSel sel = CartSel.INSTANCE;

    // Lado (mm) dos botões — densidade-independente (ver util.Px).
    private static final float STEP_BTN_MM = 7f;
    private static final float REMOVE_BTN_MM = 7f;

    private final ShoppingCn1RemoteApp app;
    private final BiConsumer<Long, Integer> onModify;
    private final Consumer<Long> onRemove;

    private Slot shell;
    private Label nameComp;
    private QtyStepper stepper;
    private Label subtotalComp;
    private IconButton removeComp;
    private Consumer<String> name;
    private Consumer<String> subtotal;
    private Boolean builtExpanded;
    private long currentId = -1;
    private int currentQty = 1;

    public CartItemCn1View(ShoppingCn1RemoteApp app, BiConsumer<Long, Integer> onModify, Consumer<Long> onRemove) {
        this.app = app;
        this.onModify = onModify;
        this.onRemove = onRemove;
    }

    @Override
    protected Container build() {
        // peças construídas uma vez (campos ligados aqui) e reusadas nos dois arranjos
        nameComp = new Label("");
        nameComp.setUIID(sel.CART_ITEM_NAME);
        nameComp.setEndsWith3Points(true); // nome longo vira "…" em vez de cortar no meio
        name = Guard.text(nameComp);

        stepper = new QtyStepper(STEP_BTN_MM,
                () -> onModify.accept(currentId, currentQty - 1),
                () -> onModify.accept(currentId, currentQty + 1));

        subtotalComp = new Label("");
        subtotalComp.setUIID(sel.CART_ITEM_SUBTOTAL);
        subtotal = Guard.text(subtotalComp);

        removeComp = new IconButton(sel.CART_REMOVE_BTN, FontImage.MATERIAL_CLOSE, 3f, Px.mm(REMOVE_BTN_MM),
                () -> onRemove.accept(currentId));

        shell = new Slot();
        return shell;
    }

    /** Arranja as peças conforme o modo (linha no expandido, empilhado no compacto); re-monta só na troca. */
    private void layout() {
        boolean expanded = app.isExpanded();
        if (builtExpanded != null && builtExpanded.booleanValue() == expanded) {
            return;
        }
        builtExpanded = Boolean.valueOf(expanded);
        shell.mount(expanded ? buildRow() : buildStacked());
    }

    /** Expandido: nome (CENTER) | [stepper subtotal ×] (EAST), numa linha só. */
    private Container buildRow() {
        return Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.CART_ITEM_ROW);
            dom.add(nameComp, BorderLayout.CENTER);
            dom.boxX(BorderLayout.EAST, east -> {
                dom.add(stepper, null);
                dom.add(subtotalComp, null);
                dom.add(removeComp, null);
            });
        });
    }

    /** Compacto: linha 1 = nome | subtotal; linha 2 = stepper | ×. */
    private Container buildStacked() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_ITEM_ROW);
            dom.border(line1 -> {
                dom.add(nameComp, BorderLayout.CENTER);
                dom.add(subtotalComp, BorderLayout.EAST);
            });
            dom.border(line2 -> {
                dom.add(stepper, BorderLayout.WEST);
                dom.add(removeComp, BorderLayout.EAST);
            });
        });
    }

    @Override
    protected void doUpdate() {
        layout();
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        currentQty = Json.intOf(m, "quantity");
        name.accept(Json.str(m, "name"));
        stepper.setValue(currentQty);
        subtotal.accept(Money.format(Json.doubleOf(m, "price") * currentQty));
    }
}
