package br.com.wdc.shopping.view.remote.shell.cn1.views.cart;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.plaf.RoundBorder;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Money;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/**
 * Cart (classId {@value #CLASS_ID}) — mirrors React: card with header and, depending on the state,
 * the <b>empty state</b> (round icon + "Your cart is empty" + "View products") or the <b>item list</b>
 * (name | stepper | subtotal | remove), with total and actions (Continue shopping / Checkout).
 */
public class CartCn1View extends AbstractCn1View {

    private static final CartSel sel = CartSel.INSTANCE;

    public static final String CLASS_ID = "7eb485e5f843";
    private static final int EVT_BUY = 1;
    private static final int EVT_BACK = 3;

    /** Side (mm) of the empty state's round icon — density-independent (see util.Px). */
    private static final float EMPTY_ICON_MM = 20f;

    private Slot body;
    private Container emptySection;
    private Container itemsSection;
    private Container list;
    private Consumer<String> total;

    public CartCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_PAGE);
            r.setScrollableY(true);
            dom.boxY(card -> {
                card.setUIID(sel.CART_CARD);
                
                dom.cardHeader(null, h -> {
                    h.setIcon(FontImage.MATERIAL_SHOPPING_BAG);
                    h.setTitle("Cart");
                    h.setSubtitle("Your selected products");
                });

                body = dom.slot();
            });
        });
        emptySection = buildEmpty();
        itemsSection = buildItems();
        return root;
    }

    private Container buildEmpty() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setUIID(sel.CART_EMPTY);
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                dom.label(l -> {
                    l.setUIID(sel.CART_EMPTY_ICON_BOX);
                    l.setPreferredSize(new Dimension(Px.mm(EMPTY_ICON_MM), Px.mm(EMPTY_ICON_MM)));
                    l.getAllStyles().setBorder(RoundBorder.create().color(Colors.ACCENT_LIGHT));
                    FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 8f);
                });
            });
            dom.label(l -> {
                l.setUIID(sel.CART_EMPTY_TITLE);
                l.setText("Your cart is empty");
            });
            dom.label(l -> {
                l.setUIID(sel.CART_EMPTY_SUB);
                l.setText("Add products to get started");
            });
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, wrap -> {
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText("View products");
                    b.addActionListener(e -> submit(EVT_BACK));
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_VIEW_MODULE, 3.5f);
                });
            });
        });
    }

    private Container buildItems() {
        return Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            list = dom.boxY(Cn1Dom.NO_CONTENT);
            dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), null, footer -> {
                footer.setUIID(sel.CART_FOOTER);
                dom.label(l -> {
                    l.setUIID(sel.CART_FOOTER_LABEL);
                    l.setText("Total:");
                });
                dom.label(l -> {
                    l.setUIID(sel.CART_FOOTER_TOTAL);
                    total = Guard.text(l);
                });
            });
            // actions in a centered FlowLayout: when compact they wrap (stack) instead of cutting
            // the text; when expanded they sit side by side. Same pattern as ProductCn1View's actions.
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                actions.setUIID(sel.CART_ACTIONS);
                dom.add(new BackButton("Continue shopping", () -> submit(EVT_BACK)), null);
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText("Checkout");
                    b.addActionListener(e -> submit(EVT_BUY));
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_CHECK_CIRCLE, 3.5f);
                });
            });
        });
    }

    @Override
    public void doUpdate() {
        // The rows are views of their own (CartItemCn1View), referenced by id in cart order
        List<Object> rowIds = Json.asList(state().get("rowsViewIds"));
        boolean empty = rowIds.isEmpty();

        body.mount(empty ? emptySection : itemsSection); // swaps only on transition (reference comparison)

        if (!empty) {
            List<Container> rows = new ArrayList<>();
            for (Object id : rowIds) {
                Container row = childElement(String.valueOf(id));
                if (row != null) {
                    rows.add(row);
                }
            }
            syncRows(rows);
            total.accept(Money.format(Json.doubleOf(state(), "total")));
        }
    }

    /** Puts the row elements in the list, in cart order; re-mounts only when the order changes.
 */
    private void syncRows(List<Container> rows) {
        boolean same = list.getComponentCount() == rows.size();
        for (int i = 0; same && i < rows.size(); i++) {
            same = list.getComponentAt(i) == rows.get(i);
        }
        if (same) {
            return;
        }
        list.removeAll();
        for (Container row : rows) {
            if (row.getParent() != null) {
                row.remove();
            }
            list.add(row);
        }
        list.revalidate();
    }
}
