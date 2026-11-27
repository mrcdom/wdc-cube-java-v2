package br.com.wdc.store.view.teavm.commons.views.cart;

import static br.com.wdc.store.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.store.view.teavm.commons.Swc.spButton;
import static br.com.wdc.store.view.teavm.commons.VNode.clsx;
import static br.com.wdc.store.view.teavm.commons.VNode.div;
import static br.com.wdc.store.view.teavm.commons.VNode.h5;
import static br.com.wdc.store.view.teavm.commons.VNode.p;
import static br.com.wdc.store.view.teavm.commons.VNode.span;

import java.util.List;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelComponents;
import br.com.wdc.store.view.teavm.commons.SelIcons;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.SharedVDomView;
import br.com.wdc.store.view.teavm.commons.VNode;

/**
 * Shared Cart view. Scope and actions injected via lambdas.
 */
public class CartSharedView extends SharedVDomView {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String HIDDEN = u.HIDDEN;
        String CARD = c.CARD_PANEL;
        String HEADER_ROW = c.CARD_HEADER_ROW;
        String HEADER_ICON_BOX = c.CARD_HEADER_ICON_BOX;
        String HEADER_ICON = clsx(icon.BAG, c.CARD_HEADER_ICON);
        String HEADER_TITLE = c.CARD_HEADER_TITLE;
        String HEADER_SUBTITLE = c.CARD_HEADER_SUBTITLE;
        String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
        String EMPTY_STATE = clsx(c.EMPTY_STATE, u.PY_48);
        String EMPTY_ICON_BOX = "cart-empty-icon-box";
        String EMPTY_ICON = clsx(icon.BAG, "cart-empty-icon");
        String EMPTY_TITLE = "cart-empty-title";
        String EMPTY_SUBTITLE = "cart-empty-subtitle";
        String FOOTER = "cart-footer";
        String FOOTER_LABEL = "cart-footer-label";
        String FOOTER_TOTAL = "cart-footer-total";
        String ACTIONS_ROW = "cart-actions-row";
        String ICON_VIEW_PRODUCTS = clsx(icon.GRID_3X3_GAP, u.MR_6);
        String ICON_FINALIZE = clsx(icon.CHECK2_CIRCLE, u.MR_6);
    }

    // -- External bindings --

    public Supplier<CartScope> scopeSupplier;
    public Runnable onBack;
    public Runnable onBuy;
    /** The rows' elements, in the cart's order: each row is a view of its own ({@link CartItemSharedView}). */
    public Supplier<List<HTMLElement>> rowElements;

    // -- Stable event listeners --

    private final EventListener<Event> backListener = evt -> onBack.run();
    private final EventListener<Event> buyListener = evt -> onBuy.run();

    // -- Render --

    @Override
    public VNode render() {
        var scope = scopeSupplier.get();
        var errorMessage = I18n.text(scope.errorMessage.get());
        var showError = !errorMessage.isEmpty();
        var rows = rowElements != null ? rowElements.get() : List.<HTMLElement>of();
        var empty = rows.isEmpty();
        var totalCost = scope.total.get() != null ? scope.total.get() : 0.0;
        var totalText = I18n.price(totalCost);

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            div(Sel.CARD).children(
              div(Sel.HEADER_ROW).children(
                div(Sel.HEADER_ICON_BOX)
                  .children(span(Sel.HEADER_ICON)),
                div().children(
                  h5(Sel.HEADER_TITLE).text(I18n.text(StoreMessages.CART_TITLE)),
                  span(Sel.HEADER_SUBTITLE).text(I18n.text(StoreMessages.CART_SUBTITLE)))),
              // Error
              div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
                span(Sel.ERROR_ICON),
                span(Sel.ERROR_TEXT).text(errorMessage)),
              // Empty cart state
              renderEmptyState(empty),
              // Cart content
              renderContent(rows, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div(empty ? Sel.EMPTY_STATE : Sel.HIDDEN).children(
          div(Sel.EMPTY_ICON_BOX)
            .children(span(Sel.EMPTY_ICON)),
          p(Sel.EMPTY_TITLE).text(I18n.text(StoreMessages.CART_EMPTY)),
          p(Sel.EMPTY_SUBTITLE).text(I18n.text(StoreMessages.CART_EMPTY_HINT)),
          spButton("accent")
            .children(span(Sel.ICON_VIEW_PRODUCTS), span().text(I18n.text(StoreMessages.CART_VIEW_PRODUCTS)))
            .on("click", backListener));
        // @formatter:on
    }

    private VNode renderContent(List<HTMLElement> rows, boolean empty, String totalText) {
        // @formatter:off
        return div(empty ? Sel.HIDDEN : "").children(
          div().children(rows.stream().map(VNode::slot).toList()),
          div(Sel.FOOTER).children(
            span(Sel.FOOTER_LABEL).text(I18n.text(StoreMessages.CART_TOTAL) + " "),
            span(Sel.FOOTER_TOTAL).text(totalText)),
          div(Sel.ACTIONS_ROW).children(
            spActionButton()
              .children(span(SelIcons.ARROW_LEFT), span().text(" " + I18n.text(StoreMessages.CART_CONTINUE_SHOPPING)))
              .on("click", backListener),
            spButton("accent", "l")
              .boolAttr("disabled", empty)
              .children(span(Sel.ICON_FINALIZE), span().text(I18n.text(StoreMessages.CART_CHECKOUT)))
              .on("click", buyListener)));
        // @formatter:on
    }
}
