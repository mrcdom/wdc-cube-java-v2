package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.IconButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.QtyStepper;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Linha do carrinho (classId {@value #CLASS_ID}): nome | stepper (− qtd +) | subtotal | remover (×). É uma view
 * própria, cujas ações são as da linha — o item é a linha, nenhum id de produto volta ao servidor; o carrinho só
 * arruma os elementos das linhas na ordem dele.
 *
 * <p>
 * As peças são construídas <b>uma vez</b> e o {@code doUpdate()} as <b>arranja</b> conforme o modo,
 * re-montando só na troca (reusa as peças). No <b>expandido</b> tudo fica numa linha (nome à esquerda,
 * controles à direita); no <b>compacto</b> os controles não cabem ao lado do nome — o card estreito
 * espremeria o nome a zero e empurraria o × para fora da borda — então empilha em duas linhas:
 * nome | subtotal em cima, stepper | × embaixo.
 * </p>
 */
public class CartItemCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "c8a1f0e2b7d4";
    private static final int EVT_REMOVE = 1;
    private static final int EVT_MODIFY = 2;

    private static final CartSel sel = CartSel.INSTANCE;

    // Lado (mm) dos botões — densidade-independente (ver util.Px).
    private static final float STEP_BTN_MM = 7f;
    private static final float REMOVE_BTN_MM = 7f;


    private Slot shell;
    private Label nameComp;
    private QtyStepper stepper;
    private Label subtotalComp;
    private IconButton removeComp;
    private Consumer<String> name;
    private Consumer<String> subtotal;
    private Boolean builtExpanded;
    private int currentQty = 1;

    public CartItemCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        // peças construídas uma vez (campos ligados aqui) e reusadas nos dois arranjos
        nameComp = new Label("");
        nameComp.setUIID(sel.CART_ITEM_NAME);
        nameComp.setEndsWith3Points(true); // nome longo vira "…" em vez de cortar no meio
        name = Guard.text(nameComp);

        stepper = new QtyStepper(STEP_BTN_MM,
                () -> modifyQuantity(currentQty - 1),
                () -> modifyQuantity(currentQty + 1));

        subtotalComp = new Label("");
        subtotalComp.setUIID(sel.CART_ITEM_SUBTOTAL);
        subtotal = Guard.text(subtotalComp);

        removeComp = new IconButton(sel.CART_REMOVE_BTN, FontImage.MATERIAL_CLOSE, 3f, Px.mm(REMOVE_BTN_MM),
                () -> submit(EVT_REMOVE));

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
    public void doUpdate() {
        layout();
        Map<String, Object> m = state();
        currentQty = Json.intOf(m, "quantity");
        name.accept(Json.str(m, "name"));
        stepper.setValue(currentQty);
        subtotal.accept(Money.format(Json.doubleOf(m, "price") * currentQty));
    }

    private void modifyQuantity(int quantity) {
        Map<String, Object> form = new HashMap<>();
        form.put("p.quantity", quantity);
        submit(EVT_MODIFY, form);
    }
}
