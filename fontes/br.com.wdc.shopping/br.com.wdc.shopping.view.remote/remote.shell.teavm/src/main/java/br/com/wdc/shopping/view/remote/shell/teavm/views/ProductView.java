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

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Product detail view.
 * State: product {id, name, image, price, description}, errorMessage, errorCode.
 */
public class ProductView extends AbstractRemoteView {

    public static final String VIEW_ID = "48b693f67410";

    private static final int ON_BACK = 1;
    private static final int ON_ADD_TO_CART = 2;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = CssUtility.PAGE_SCROLL_ROOT;
        String WRAPPER = CssUtility.PAGE_WRAPPER;
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
        String ICON_ADD_CART = clsx(CssIcons.BAG_PLUS, CssUtility.MR_6);
        String ERROR_VISIBLE = clsx(CssComponents.ALERT_ERROR, CssUtility.MT_12);
        String HIDDEN = CssUtility.HIDDEN;
        String ERROR_ICON = clsx(CssIcons.EXCLAMATION_CIRCLE, CssComponents.ALERT_ERROR_ICON);
        String ERROR_TEXT = CssComponents.ALERT_ERROR_TEXT;
    }

    private int quantity = 1;
    private String currentDescription = "";

    public ProductView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        Map<String, Object> product = scope.getMap("product");
        String imageUrl = "";
        String name = "";
        String price = "";
        String description = "";

        if (!product.isEmpty()) {
            var img = product.get("image");
            imageUrl = img != null ? "/" + img : "";
            var n = product.get("name");
            name = n != null ? n.toString() : "";
            var p = product.get("price");
            price = p instanceof Number num ? "R$ " + String.format("%.2f", num.doubleValue()) : "";
            var d = product.get("description");
            description = d != null ? d.toString() : "";
        }

        final String desc = description;

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            h5(Css.TITLE).text(name),
            spDivider("s").cls(Css.DIVIDER),
            // Description card
            div(Css.DESC_CARD).children(
              div(Css.DESC_TEXT)
                .ref(el -> {
                    if (!desc.equals(this.currentDescription)) {
                        el.setInnerHTML(desc);
                        this.currentDescription = desc;
                    }
                })),
            // Price + Image row
            div(Css.PRICE_IMAGE_ROW).children(
              div(Css.PRICE_COL).children(
                span(Css.PRICE_BADGE).text(price),
                div(Css.QTY_ROW).children(
                  span(Css.QTY_LABEL).text("Qtd:"),
                  spActionButton("s")
                    .children(span(CssIcons.DASH))
                    .on("click", evt -> { if (this.quantity > 1) { this.quantity--; forceUpdate(); } }),
                  span(Css.QTY_VALUE).text(String.valueOf(this.quantity)),
                  spActionButton("s")
                    .children(span(CssIcons.PLUS))
                    .on("click", evt -> { this.quantity++; forceUpdate(); }))),
              div(Css.IMAGE_BOX).children(
                img().attr("alt", name).attr("src", imageUrl).cls(Css.IMAGE))),
            // Action buttons
            div(Css.ACTIONS_ROW).children(
              spActionButton()
                .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar"))
                .on("click", evt -> submit(ON_BACK)),
              spButton("accent", "l")
                .children(span(Css.ICON_ADD_CART), span().text("Adicionar ao Carrinho"))
                .on("click", evt -> { setFormField("p.quantity", this.quantity); submit(ON_ADD_TO_CART); }))),
          // Error
          div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
            span(Css.ERROR_ICON),
            span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")));
        // @formatter:on
    }
}
