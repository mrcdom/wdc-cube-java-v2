package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.Swc.spDivider;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.img;
import static br.com.wdc.framework.vdom.VNode.span;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;

public class ProductViewVDom extends AbstractVDomView<ProductPresenter> {

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

    private final ProductViewState state;
    private int quantity = 1;
    private String currentDescription = "";

    public ProductViewVDom(ProductPresenter presenter) {
        super("product", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode != 0) {
            showError = true;
            errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            showError = false;
            errorMessage = "";
        }

        var product = this.state.product;
        var imageUrl = "";
        var name = "";
        var price = "";
        final String description;

        if (product != null) {
            imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
            name = product.name != null ? product.name : "";
            price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";
            description = product.description != null ? product.description : "";
        } else {
            description = "";
        }

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            // Title
            h5(Css.TITLE).text(name),
            // Divider
            spDivider("s").cls(Css.DIVIDER),
            // Description card
            div(Css.DESC_CARD).children(
              div(Css.DESC_TEXT)
                .ref(el -> {
                    if (!description.equals(this.currentDescription)) {
                        el.setInnerHTML(description);
                        this.currentDescription = description;
                    }
                })),
            // Price + Image row
            div(Css.PRICE_IMAGE_ROW).children(
              // Left: price + quantity (centered)
              div(Css.PRICE_COL).children(
                span(Css.PRICE_BADGE).text(price),
                // Quantity stepper
                div(Css.QTY_ROW).children(
                  span(Css.QTY_LABEL).text("Qtd:"),
                  spActionButton("s")
                    .children(span(CssIcons.DASH))
                    .on("click", evt -> { if (this.quantity > 1) { this.quantity--; update(); } }),
                  span(Css.QTY_VALUE).text(String.valueOf(this.quantity)),
                  spActionButton("s")
                    .children(span(CssIcons.PLUS))
                    .on("click", evt -> { this.quantity++; update(); }))),
              // Right: product image
              div(Css.IMAGE_BOX).children(
                img().attr("alt", name).attr("src", imageUrl).cls(Css.IMAGE))),
            // Action buttons row
            div(Css.ACTIONS_ROW).children(
              spActionButton()
                .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar"))
                .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
              spButton("accent", "l")
                .children(span(Css.ICON_ADD_CART), span().text("Adicionar ao Carrinho"))
                .on("click", evt -> safeAction("AddToCart", () -> this.presenter.onAddToCart(this.quantity))))),
          // Error
          div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
            span(Css.ERROR_ICON),
            span(Css.ERROR_TEXT).text(errorMessage)));
        // @formatter:on
    }
}
