package br.com.wdc.shopping.view.teavm.commons.views.cart;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spButton;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.h5;
import static br.com.wdc.shopping.view.teavm.commons.VNode.p;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared Cart view. State and actions injected via lambdas.
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
        String ITEM_ROW = "cart-item-row";
        String ITEM_NAME = "cart-item-name";
        String STEPPER_ROW = "cart-stepper-row";
        String STEPPER_MINUS_ICON = clsx(icon.DASH, "cart-stepper-icon");
        String STEPPER_PLUS_ICON = clsx(icon.PLUS, "cart-stepper-icon");
        String STEPPER_VALUE = "cart-stepper-value";
        String ITEM_SUBTOTAL = "cart-item-subtotal";
        String REMOVE_ICON = clsx(icon.X_LG, "cart-remove-icon");
        String ICON_VIEW_PRODUCTS = clsx(icon.GRID_3X3_GAP, u.MR_6);
        String ICON_FINALIZE = clsx(icon.CHECK2_CIRCLE, u.MR_6);
    }

    // -- External bindings --

    public Supplier<CartViewState> stateSupplier;
    public Runnable onBack;
    public Runnable onBuy;
    public BiConsumer<Long, Integer> onModifyQuantity;
    public Consumer<Long> onRemoveProduct;

    // -- Stable event listeners --

    private final EventListener<Event> backListener = evt -> { if (onBack != null) onBack.run(); };
    private final EventListener<Event> buyListener = evt -> { if (onBuy != null) onBuy.run(); };

    private EventListener<Event> mkOnModifyQuantity(long id, int qty) {
        return evt -> { if (onModifyQuantity != null) onModifyQuantity.accept(id, qty); };
    }

    private EventListener<Event> mkOnRemove(long id) {
        return evt -> { if (onRemoveProduct != null) onRemoveProduct.accept(id); };
    }

    // -- Render --

    @Override
    public VNode render() {
        var state = stateSupplier.get();
        var errorMessage = state.errorMessage;
        var showError = errorMessage != null && !errorMessage.isEmpty();
        var items = state.items;
        var empty = items == null || items.isEmpty();
        var totalCost = computeTotalCost(items);
        var totalText = "R$ " + String.format("%.2f", totalCost);

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            div(Sel.CARD).children(
              div(Sel.HEADER_ROW).children(
                div(Sel.HEADER_ICON_BOX)
                  .children(span(Sel.HEADER_ICON)),
                div().children(
                  h5(Sel.HEADER_TITLE).text("Carrinho"),
                  span(Sel.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
                span(Sel.ERROR_ICON),
                span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // Empty cart state
              renderEmptyState(empty),
              // Cart content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div(empty ? Sel.EMPTY_STATE : Sel.HIDDEN).children(
          div(Sel.EMPTY_ICON_BOX)
            .children(span(Sel.EMPTY_ICON)),
          p(Sel.EMPTY_TITLE).text("Carrinho vazio"),
          p(Sel.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span(Sel.ICON_VIEW_PRODUCTS), span().text("Ver produtos"))
            .on("click", backListener));
        // @formatter:on
    }

    private VNode renderContent(List<CartItem> items, boolean empty, String totalText) {
        // @formatter:off
        return div(empty ? Sel.HIDDEN : "").children(
          div().children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),
          div(Sel.FOOTER).children(
            span(Sel.FOOTER_LABEL).text("Total: "),
            span(Sel.FOOTER_TOTAL).text(totalText)),
          div(Sel.ACTIONS_ROW).children(
            spActionButton()
              .children(span(SelIcons.ARROW_LEFT), span().text(" Continuar comprando"))
              .on("click", backListener),
            spButton("accent", "l")
              .boolAttr("disabled", empty)
              .children(span(Sel.ICON_FINALIZE), span().text("Finalizar pedido"))
              .on("click", buyListener)));
        // @formatter:on
    }

    private VNode renderItem(CartItem item) {
        var name = item.name != null ? item.name : "";
        var subtotal = "R$ " + String.format("%.2f", item.price * item.quantity);
        var key = String.valueOf(item.id);

        // @formatter:off
        return div(Sel.ITEM_ROW).key(key).children(
          span(Sel.ITEM_NAME).text(name),
          div(Sel.STEPPER_ROW).children(
            spActionButton("s")
              .children(span(Sel.STEPPER_MINUS_ICON))
              .on("click", useCallback("minus-" + key + "-" + item.quantity, mkOnModifyQuantity(item.id, item.quantity - 1))),
            span(Sel.STEPPER_VALUE).text(String.valueOf(item.quantity)),
            spActionButton("s")
              .children(span(Sel.STEPPER_PLUS_ICON))
              .on("click", useCallback("plus-" + key + "-" + item.quantity, mkOnModifyQuantity(item.id, item.quantity + 1)))),
          span(Sel.ITEM_SUBTOTAL).text(subtotal),
          spActionButton("s")
            .children(span(Sel.REMOVE_ICON))
            .on("click", useCallback("remove-" + key, mkOnRemove(item.id))));
        // @formatter:on
    }

    private double computeTotalCost(List<CartItem> items) {
        if (items == null) return 0;
        double total = 0;
        for (var item : items) {
            total += item.price * item.quantity;
        }
        return total;
    }
}
