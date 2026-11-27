package br.com.wdc.store.view.remote.shell.cn1.views.product;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.store.view.remote.shell.cn1.StoreCn1RemoteApp;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.store.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.store.view.remote.shell.cn1.i18n.StoreMessages;
import br.com.wdc.store.view.remote.shell.cn1.i18n.Texts;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Images;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;
import br.com.wdc.store.view.remote.shell.cn1.util.Money;
import br.com.wdc.store.view.remote.shell.cn1.widgets.BackButton;
import br.com.wdc.store.view.remote.shell.cn1.widgets.HtmlText;
import br.com.wdc.store.view.remote.shell.cn1.widgets.QtyStepper;
import br.com.wdc.store.view.remote.shell.cn1.widgets.Slot;

/**
 * Product detail (classId {@value #CLASS_ID}) — mirrors the React design: title + divider,
 * description card, a row with price/quantity on the left and image on the right, and the action bar
 * (Back / Add to Cart). The quantity is controlled locally (stepper − value +).
 */
public class ProductCn1View extends AbstractCn1View {

    private static final ProductSel sel = ProductSel.INSTANCE;

    public static final String CLASS_ID = "48b693f67410";
    private static final int EVT_BACK = 1;
    private static final int EVT_ADD_TO_CART = 2;
    // Dimensions in mm (density-independent) — see util.Px.
    private static final float DETAIL_MM = 28f;
    private static final float DIVIDER_H_MM = 0.6f;
    /** Side (mm) of the +/- quantity buttons — avoids the Button's huge minimum height. */
    private static final float QTY_BTN_MM = 8f;

    private Consumer<String> name;
    private Consumer<String> price;
    private Label image;
    private QtyStepper stepper;
    private HtmlText description;
    private Slot rowSlot;
    private Container priceQty;
    private Container imageBox;
    private Boolean builtExpanded;
    private long currentId = -1;
    private String lastHtml = "";
    private int quantity = 1;

    public ProductCn1View(String vsid, BridgeSession session, StoreCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        // pieces reused in both modes (fields bound here); the arrangement is decided in doUpdate
        imageBox = buildImageBox();
        priceQty = buildPriceQty();
        Container root = Cn1Dom.render(BoxLayout.y(), (dom, r) -> {
            r.setScrollableY(true);
            r.setUIID(sel.PRODUCT_PAGE);
            dom.label(l -> {
                l.setUIID(sel.PRODUCT_TITLE);
                name = Guard.text(l);
            });
            Label divider = dom.label(l -> l.setUIID(sel.PRODUCT_DIVIDER));
            divider.setPreferredH(Px.mm(DIVIDER_H_MM));

            // description card
            dom.boxY(card -> {
                card.setUIID(sel.PRODUCT_DESC_CARD);
                description = dom.add(new HtmlText(sel.PRODUCT_DESC_TEXT), null);
            });

            // price/quantity + image — arrangement (side by side vs stacked) decided in layout()
            rowSlot = dom.slot();
            rowSlot.setUIID(sel.PRODUCT_PRICE_IMAGE_ROW);

            // actions: Back + Add to Cart
            dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, actions -> {
                dom.add(new BackButton(Texts.text(StoreMessages.PRODUCT_BACK), () -> submit(EVT_BACK)), null);
                dom.button(b -> {
                    b.setUIID(sel.PRIMARY_BUTTON);
                    b.setText(Texts.text(StoreMessages.PRODUCT_ADD_TO_CART));
                    b.addActionListener(e -> addToCart());
                    FontImage.setMaterialIcon(b, FontImage.MATERIAL_ADD_SHOPPING_CART, 3.5f);
                });
            });
        });
        updateQty();
        return root;
    }

    /**
     * Arranges price/qty + image according to the mode: side by side when expanded; stacked (image on top)
     * when compact, otherwise the sum of the widths cuts the image on a phone. Re-mounts only on a mode
     * switch, reusing the pieces (preserves the quantity chosen in the stepper).
     */
    private void layout() {
        boolean expanded = app.isExpanded();
        if (builtExpanded != null && builtExpanded.booleanValue() == expanded) {
            return;
        }
        builtExpanded = Boolean.valueOf(expanded);
        rowSlot.mount(Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, w) -> {
            if (expanded) {
                dom.boxX(group -> {
                    dom.add(priceQty, null);
                    dom.add(imageBox, null);
                });
            } else {
                dom.boxY(stack -> {
                    dom.add(imageBox, null);
                    dom.add(priceQty, null);
                });
            }
        }));
    }

    /** Product image box (gradient background), centered. */
    private Container buildImageBox() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, box) -> {
            box.setUIID(sel.PRODUCT_IMAGE_BOX);
            image = dom.label(l -> { });
        });
    }

    /** Price badge + "Qty: − value +" row. The qty uses BoxLayout.x (centers vertically, like the
     *  cart stepper); the outer FlowLayout centers it horizontally. */
    private Container buildPriceQty() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, priceWrap) -> {
            dom.boxY(priceCol -> {
                dom.label(l -> {
                    l.setUIID(sel.PRODUCT_PRICE_BADGE);
                    price = Guard.text(l);
                });
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, qtyWrap -> {
                    dom.boxX(qtyRow -> {
                        dom.label(l -> {
                            l.setUIID(sel.QTY_LABEL);
                            l.setText(Texts.text(StoreMessages.CART_QUANTITY));
                        });
                        stepper = dom.add(new QtyStepper(QTY_BTN_MM,
                                () -> changeQty(-1), () -> changeQty(1)), null);
                    });
                });
            });
        });
    }

    private void changeQty(int delta) {
        int next = quantity + delta;
        if (next >= 1) {
            quantity = next;
            updateQty();
        }
    }

    private void updateQty() {
        stepper.setValue(quantity);
    }

    private void addToCart() {
        Map<String, Object> form = new HashMap<>();
        form.put("p.quantity", quantity);
        submit(EVT_ADD_TO_CART, form);
    }

    @Override
    public void doUpdate() {
        showError(); // the presenter's error, if any, as a toast
        layout(); // adapts the price/image arrangement to the mode (re-mounts only on a switch)

        Map<String, Object> p = Json.asMap(scope().get("product"));
        long id = Json.longOf(p, "id");
        if (id != currentId) {
            currentId = id;
            image.setIcon(Images.product(id, Px.mm(DETAIL_MM)));
        }
        name.accept(Json.str(p, "name"));
        price.accept(Money.format(Json.doubleOf(p, "price")));
        String html = Json.str(p, "description");
        if (!html.equals(lastHtml)) {
            lastHtml = html;
            description.setHtml(html);
        }
    }
}
