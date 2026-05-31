package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.Swc.spDivider;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.img;
import static br.com.wdc.framework.vdom.VNode.span;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.vdom.SelComponents;
import br.com.wdc.framework.vdom.SelIcons;
import br.com.wdc.framework.vdom.SelUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;

public class ProductView extends AbstractVDomView<ProductPresenter> {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String HIDDEN = u.HIDDEN;
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
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
    }

    private final ProductViewState state;
    private int quantity = 1;
    private String currentDescription = "";

    // Stable event listeners
    private final EventListener<Event> onDecrement = evt -> { if (this.quantity > 1) { this.quantity--; update(); } };
    private final EventListener<Event> onIncrement = evt -> { this.quantity++; update(); };
    private final EventListener<Event> onBack = evt -> safeAction("Back", this.presenter::onOpenProducts);
    private final EventListener<Event> onAddToCart = evt -> safeAction("AddToCart", () -> this.presenter.onAddToCart(this.quantity));

    private record ProductData(String name, String image, String price, String description) {}

    public ProductView(ProductPresenter presenter) {
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

        var product = getProductData();

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
            // Action buttons row
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
            span(Sel.ERROR_TEXT).text(errorMessage)));
        // @formatter:on
    }

    private ProductData getProductData() {
        var p = this.state.product;
        if (p == null) {
            return new ProductData("", "", "", "");
        }
        var name = p.name != null ? p.name : "";
        var image = p.image != null ? app.resolveImageUrl(p.image) : "";
        var price = p.price > 0 ? "R$ " + String.format("%.2f", p.price) : "";
        var description = p.description != null ? p.description : "";
        return new ProductData(name, image, price, description);
    }
}
