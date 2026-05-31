package br.com.wdc.shopping.view.teavm.commons.views.product;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spDivider;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.h5;
import static br.com.wdc.shopping.view.teavm.commons.VNode.img;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared Product detail view. Has local quantity state.
 */
public class ProductSharedView extends SharedVDomView {

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

    // -- External bindings --

    public Supplier<ProductViewState> stateSupplier;
    public Runnable onBack;
    public Consumer<Integer> onAddToCart;
    public UnaryOperator<String> imageResolver = path -> "/" + path;
    public Runnable requestUpdate;

    // -- Local state --

    private int quantity = 1;
    private String currentDescription = "";

    // -- Stable event listeners --

    private final EventListener<Event> onDecrement = evt -> {
        if (this.quantity > 1) {
            this.quantity--;
            if (requestUpdate != null) requestUpdate.run();
        }
    };
    private final EventListener<Event> onIncrement = evt -> {
        this.quantity++;
        if (requestUpdate != null) requestUpdate.run();
    };
    private final EventListener<Event> backListener = evt -> { if (onBack != null) onBack.run(); };
    private final EventListener<Event> addToCartListener = evt -> { if (onAddToCart != null) onAddToCart.accept(this.quantity); };

    // -- Render --

    @Override
    public VNode render() {
        var state = stateSupplier.get();
        var errorMessage = state.errorMessage;
        var showError = errorMessage != null && !errorMessage.isEmpty();

        String name = "";
        String image = "";
        String price = "";
        String description = "";
        if (state.product != null) {
            var p = state.product;
            name = p.name != null ? p.name : "";
            image = p.image != null ? imageResolver.apply(p.image) : "";
            price = p.price > 0 ? "R$ " + String.format("%.2f", p.price) : "";
            description = p.description != null ? p.description : "";
        }

        final String descRef = description;

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            h5(Sel.TITLE).text(name),
            spDivider("s").cls(Sel.DIVIDER),
            // Description card
            div(Sel.DESC_CARD).children(
              div(Sel.DESC_TEXT)
                .ref(el -> {
                    if (!descRef.equals(this.currentDescription)) {
                        el.setInnerHTML(descRef);
                        this.currentDescription = descRef;
                    }
                })),
            // Price + Image row
            div(Sel.PRICE_IMAGE_ROW).children(
              div(Sel.PRICE_COL).children(
                span(Sel.PRICE_BADGE).text(price),
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
                img().attr("alt", name).attr("src", image).cls(Sel.IMAGE))),
            // Action buttons row
            div(Sel.ACTIONS_ROW).children(
              spActionButton()
                .children(span(SelIcons.ARROW_LEFT), span().text(" Voltar"))
                .on("click", backListener),
              spButton("accent", "l")
                .children(span(Sel.ICON_ADD_CART), span().text("Adicionar ao Carrinho"))
                .on("click", addToCartListener))),
          // Error
          div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
            span(Sel.ERROR_ICON),
            span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")));
        // @formatter:on
    }
}
