package br.com.wdc.store.view.remote.shell.cn1.views.cart;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.store.view.remote.shell.cn1.StoreCn1RemoteApp;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.store.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;
import br.com.wdc.store.view.remote.shell.cn1.util.Money;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;
import br.com.wdc.store.view.remote.shell.cn1.widgets.IconButton;
import br.com.wdc.store.view.remote.shell.cn1.widgets.QtyStepper;
import br.com.wdc.store.view.remote.shell.cn1.widgets.Slot;

/**
 * Cart row (classId {@value #CLASS_ID}): name | stepper (− qty +) | subtotal | remove (×). It is a view of
 * its own, whose actions are the row's — the item is the row, no product id goes back to the server; the cart
 * only arranges the row elements in its order.
 *
 * <p>
 * The pieces are built <b>once</b> and {@code doUpdate()} <b>arranges</b> them according to the mode,
 * re-mounting only on a switch (reuses the pieces). When <b>expanded</b> everything sits on one line (name on
 * the left, controls on the right); when <b>compact</b> the controls do not fit beside the name — the narrow
 * card would squeeze the name to zero and push the × past the edge — so it stacks into two lines:
 * name | subtotal on top, stepper | × below.
 * </p>
 */
public class CartItemCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "c8a1f0e2b7d4";
    private static final int EVT_REMOVE = 1;
    private static final int EVT_MODIFY = 2;

    private static final CartSel sel = CartSel.INSTANCE;

    // Side (mm) of the buttons — density-independent (see util.Px).
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

    public CartItemCn1View(String vsid, BridgeSession session, StoreCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        // pieces built once (fields bound here) and reused in both arrangements
        nameComp = new Label("");
        nameComp.setUIID(sel.CART_ITEM_NAME);
        nameComp.setEndsWith3Points(true); // a long name becomes "…" instead of being cut in the middle
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

    /** Arranges the pieces according to the mode (row when expanded, stacked when compact); re-mounts only on a switch. */
    private void layout() {
        boolean expanded = app.isExpanded();
        if (builtExpanded != null && builtExpanded.booleanValue() == expanded) {
            return;
        }
        builtExpanded = Boolean.valueOf(expanded);
        shell.mount(expanded ? buildRow() : buildStacked());
    }

    /** Expanded: name (CENTER) | [stepper subtotal ×] (EAST), on a single line. */
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

    /** Compact: line 1 = name | subtotal; line 2 = stepper | ×.
 */
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
        Map<String, Object> m = scope();
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
