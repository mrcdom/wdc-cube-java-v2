package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.Swc.spDivider;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.img;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.Map;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.vdom.SelComponents;
import br.com.wdc.framework.vdom.SelIcons;
import br.com.wdc.framework.vdom.SelUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

/**
 * Product detail view. State: product {id, name, image, price, description}, errorMessage, errorCode.
 */
public class ProductView extends AbstractRemoteView {

    public static final String VIEW_ID = "48b693f67410";

    private static final int ON_BACK = 1;
    private static final int ON_ADD_TO_CART = 2;

    @SuppressWarnings({ "java:S1214", "static-access" })
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String TITLE = "product-title";
        String DIVIDER = "product-divider";
        String DESC_CARD = "product-desc-card";
        String DESC_TEXT = "product-desc-text";
        String PRICE_IMAGE_ROW = "product-price-image-row";
        String PRICE_COL = "product-price-col";
        String PRICE_BADGE = "product-price-badge";
        String QTY_ROW = "product-qty-row";
        String QTY_LABEL = "product-qty-label";
        String QTY_VALUE = "product-qty-value";
        String IMAGE_BOX = "product-image-box";
        String IMAGE = "product-image";
        String ACTIONS_ROW = "product-actions-row";
        String ICON_ADD_CART = clsx(icon.BAG_PLUS, u.MR_6);
        String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MT_12);
        String HIDDEN = u.HIDDEN;
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
    }

    // Stable event listeners
    private final EventListener<Event> onDecrement = evt -> {
        if (this.quantity > 1) {
            this.quantity--;
            forceUpdate();
        }
    };
    private final EventListener<Event> onIncrement = evt -> {
        this.quantity++;
        forceUpdate();
    };
    private final EventListener<Event> onBack = evt -> submit(ON_BACK);
    private final EventListener<Event> onAddToCart = evt -> {
        setFormField("p.quantity", this.quantity);
        submit(ON_ADD_TO_CART);
    };

    private int quantity = 1;
    private String currentDescription = "";

    private record Product(Long id, String name, String image, String price, String description) {
    }

    public ProductView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        var product = getProduct(scope);

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            h5(Sel.TITLE).text(product.name()),
            spDivider("s").cls(Sel.DIVIDER),
            // Description card
            div(Sel.DESC_CARD).children(
              div(Sel.DESC_TEXT)
                .ref(el -> {
                    if (!product.description().equals(this.currentDescription)) {
                        el.setInnerHTML(product.description());
                        this.currentDescription = product.description();
                    }
                })),
            // Price + Image row
            div(Sel.PRICE_IMAGE_ROW).children(
              div(Sel.PRICE_COL).children(
                span(Sel.PRICE_BADGE).text(product.price()),
                div(Sel.QTY_ROW).children(
                  span(Sel.QTY_LABEL).text("Qtd:"),
                  spActionButton("s")
                    .children(span(SelIcons.DASH))
                    .on("click", onDecrement),
                  span(Sel.QTY_VALUE).text(String.valueOf(this.quantity)),
                  spActionButton("s")
                    .children(span(SelIcons.PLUS))
                    .on("click", onIncrement))),
              div(Sel.IMAGE_BOX).children(
                img().attr("alt", product.name()).attr("src", product.image()).cls(Sel.IMAGE))),
            // Action buttons
            div(Sel.ACTIONS_ROW).children(
              spActionButton()
                .children(span(SelIcons.ARROW_LEFT), span().text(" Voltar"))
                .on("click", onBack),
              spButton("accent", "l")
                .children(span(Sel.ICON_ADD_CART), span().text("Adicionar ao Carrinho"))
                .on("click", onAddToCart))),
          // Error
          div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
            span(Sel.ERROR_ICON),
            span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")));
        // @formatter:on
    }

    // :: State mapping helpers

    private Product getProduct(ViewScope scope) {
        Map<String, Object> map = scope.getMap("product");
        if (map.isEmpty()) {
            return new Product(-1L, "", "", "", "");
        }
        var id = CoerceUtils.asLong(map.get("id"));
        var name = CoerceUtils.asString(map.get("name"), "");
        var rawImage = CoerceUtils.asString(map.get("image"));
        var image = rawImage != null ? "/" + rawImage : "";
        var priceVal = CoerceUtils.asNumber(map.get("price"));
        var price = priceVal != null ? "R$ " + String.format("%.2f", priceVal.doubleValue()) : "";
        var description = CoerceUtils.asString(map.get("description"), "");
        return new Product(id, name, image, price, description);
    }
}
