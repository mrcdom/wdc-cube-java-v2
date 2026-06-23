package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractItemCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.IconButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.QtyStepper;

/**
 * Linha do carrinho: nome | stepper (− qtd +) | subtotal | remover (×).
 *
 * <p>
 * No <b>expandido</b> (tablet/desktop) tudo fica numa linha (nome à esquerda, controles à direita).
 * No <b>compacto</b> (telefone) os controles não cabem ao lado do nome — o card estreito espremeria
 * o nome a zero e empurraria o × para fora da borda — então empilha em duas linhas: nome | subtotal
 * em cima, stepper | × embaixo. Mesmo padrão de empilhar-no-compacto do {@code ProductCn1View}.
 * </p>
 */
public class CartItemCn1View extends AbstractItemCn1View<Object> {

    private static final CartSel sel = CartSel.INSTANCE;

    // Lado (mm) dos botões — densidade-independente (ver util.Px).
    private static final float STEP_BTN_MM = 7f;
    private static final float REMOVE_BTN_MM = 7f;

    private final boolean expanded;
    private final BiConsumer<Long, Integer> onModify;
    private final Consumer<Long> onRemove;
    private Consumer<String> name;
    private QtyStepper stepper;
    private Consumer<String> subtotal;
    private long currentId = -1;
    private int currentQty = 1;

    public CartItemCn1View(boolean expanded, BiConsumer<Long, Integer> onModify, Consumer<Long> onRemove) {
        this.expanded = expanded;
        this.onModify = onModify;
        this.onRemove = onRemove;
    }

    @Override
    protected Container build() {
        return expanded ? buildRow() : buildStacked();
    }

    /** Expandido: nome (CENTER) | [stepper subtotal ×] (EAST), numa linha só. */
    private Container buildRow() {
        return Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            r.setUIID(sel.CART_ITEM_ROW);
            nameLabel(dom, BorderLayout.CENTER);
            dom.boxX(BorderLayout.EAST, east -> {
                buildStepper(dom, null);
                subtotalLabel(dom, null);
                dom.add(removeBtn(), null);
            });
        });
    }

    /** Compacto: linha 1 = nome | subtotal; linha 2 = stepper | ×. */
    private Container buildStacked() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_ITEM_ROW);
            dom.border(line1 -> {
                nameLabel(dom, BorderLayout.CENTER);
                subtotalLabel(dom, BorderLayout.EAST);
            });
            dom.border(line2 -> {
                buildStepper(dom, BorderLayout.WEST);
                dom.add(removeBtn(), BorderLayout.EAST);
            });
        });
    }

    private void nameLabel(Cn1Dom dom, Object constraint) {
        dom.label(constraint, l -> {
            l.setUIID(sel.CART_ITEM_NAME);
            l.setEndsWith3Points(true); // nome longo vira "…" em vez de cortar no meio
            name = Guard.text(l);
        });
    }

    private void buildStepper(Cn1Dom dom, Object constraint) {
        stepper = dom.add(new QtyStepper(STEP_BTN_MM,
                () -> onModify.accept(currentId, currentQty - 1),
                () -> onModify.accept(currentId, currentQty + 1)), constraint);
    }

    private void subtotalLabel(Cn1Dom dom, Object constraint) {
        dom.label(constraint, l -> {
            l.setUIID(sel.CART_ITEM_SUBTOTAL);
            subtotal = Guard.text(l);
        });
    }

    private IconButton removeBtn() {
        return new IconButton(sel.CART_REMOVE_BTN, FontImage.MATERIAL_CLOSE, 3f, Px.mm(REMOVE_BTN_MM),
                () -> onRemove.accept(currentId));
    }

    @Override
    protected void doUpdate() {
        Map<String, Object> m = Json.asMap(data);
        currentId = Json.longOf(m, "id");
        currentQty = Json.intOf(m, "quantity");
        name.accept(Json.str(m, "name"));
        stepper.setValue(currentQty);
        subtotal.accept(Money.format(Json.doubleOf(m, "price") * currentQty));
    }
}
