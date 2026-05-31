package br.com.wdc.shopping.view.remote.shell.teavm.views.cart;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spButton;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.h5;
import static br.com.wdc.shopping.view.teavm.commons.VNode.p;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

/**
 * Cart view. State: items (array of {id, name, price, quantity}), errorMessage.
 */
public class CartView extends AbstractRemoteView {

    public static final String VIEW_ID = "7eb485e5f843";

    private static final int ON_BUY = 1;
    private static final int ON_REMOVE = 2;
    private static final int ON_BACK = 3;
    private static final int ON_MODIFY_QUANTITY = 4;

    @SuppressWarnings({ "java:S1214", "static-access" })
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
        String HEADER_ICON = clsx(icon.BAG, SelComponents.CARD_HEADER_ICON);
        String HEADER_TITLE = c.CARD_HEADER_TITLE;
        String HEADER_SUBTITLE = c.CARD_HEADER_SUBTITLE;
        String ERROR_VISIBLE = clsx(c.ALERT_ERROR, u.MB_12);
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;
        String EMPTY_STATE_48 = clsx(SelComponents.EMPTY_STATE, u.PY_48);
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
        String STEPPER_MINUS_ICON = clsx(SelIcons.DASH, "cart-stepper-icon");
        String STEPPER_PLUS_ICON = clsx(SelIcons.PLUS, "cart-stepper-icon");
        String STEPPER_VALUE = "cart-stepper-value";
        String ITEM_SUBTOTAL = "cart-item-subtotal";
        String REMOVE_ICON = clsx(SelIcons.X_LG, "cart-remove-icon");
        String ICON_VIEW_PRODUCTS = clsx(SelIcons.GRID_3X3_GAP, u.MR_6);
        String ICON_FINALIZE = clsx(SelIcons.CHECK2_CIRCLE, u.MR_6);
    }

    record Item(Long id, String name, Double price, Integer qty) {}

    // Stable event listeners
    private final EventListener<Event> onBack = evt -> submit(ON_BACK);
    private final EventListener<Event> onBuy = evt -> submit(ON_BUY);

    private EventListener<Event> mkOnModifyQuantity(Long id, Integer qty) {
        return evt -> {
            setFormField("p.productId", id);
            setFormField("p.quantity", qty);
            submit(ON_MODIFY_QUANTITY);
        };
    }

    private EventListener<Event> mkOnRemove(Long id) {
        return evt -> {
            setFormField("p.productId", id);
            submit(ON_REMOVE);
        };
    }

    public CartView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        var errorMessage = scope.getString("errorMessage");
        var showError = errorMessage != null && !errorMessage.isEmpty();

        var items = getItems(scope);
        var empty = items.isEmpty();
        var totalCost = computeTotal(items);
        var totalText = "R$ " + String.format("%.2f", totalCost);

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            div(Sel.CARD).children(
              div(Sel.HEADER_ROW).children(
                div(Sel.HEADER_ICON_BOX).children(
                  span(Sel.HEADER_ICON)),
                div().children(
                  h5(Sel.HEADER_TITLE).text("Carrinho"),
                  span(Sel.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
                span(Sel.ERROR_ICON),
                span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // Empty state
              renderEmptyState(empty),
              // Content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div(empty ? Sel.EMPTY_STATE_48 : Sel.HIDDEN).children(
          div(Sel.EMPTY_ICON_BOX).children(
            span(Sel.EMPTY_ICON)),
          p(Sel.EMPTY_TITLE).text("Carrinho vazio"),
          p(Sel.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span(Sel.ICON_VIEW_PRODUCTS), span().text("Ver produtos"))
            .on("click", onBack));
        // @formatter:on
    }

    private VNode renderContent(List<Item> items, boolean empty, String totalText) {
        // @formatter:off
        return div(empty ? Sel.HIDDEN : "").children(
          div().children(items.stream().map(this::renderItem).toList()),
          div(Sel.FOOTER).children(
            span(Sel.FOOTER_LABEL).text("Total: "),
            span(Sel.FOOTER_TOTAL).text(totalText)),
          div(Sel.ACTIONS_ROW).children(
            spActionButton()
              .children(span(SelIcons.ARROW_LEFT), span().text(" Continuar comprando"))
              .on("click", onBack),
            spButton("accent", "l")
              .boolAttr("disabled", empty)
              .children(span(Sel.ICON_FINALIZE), span().text("Finalizar pedido"))
              .on("click", onBuy)));
        // @formatter:on
    }

    private VNode renderItem(Item item) {
        var subtotal = "R$ " + String.format("%.2f", item.price * item.qty);
        var key = item.id.toString();

        // @formatter:off
        return div(Sel.ITEM_ROW).key(key).children(
          span(Sel.ITEM_NAME).text(item.name),
          // Quantity stepper
          div(Sel.STEPPER_ROW).children(
            spActionButton("s")
              .children(span(Sel.STEPPER_MINUS_ICON))
              .on("click", useCallback("minus-" + key + "-" + item.qty, mkOnModifyQuantity(item.id, item.qty - 1))),
            span(Sel.STEPPER_VALUE).text(String.valueOf(item.qty)),
            spActionButton("s")
              .children(span(Sel.STEPPER_PLUS_ICON))
              .on("click", useCallback("plus-" + key + "-" + item.qty, mkOnModifyQuantity(item.id, item.qty + 1)))),
          span(Sel.ITEM_SUBTOTAL).text(subtotal),
          // Remove button
          spActionButton("s")
            .children(span(Sel.REMOVE_ICON))
            .on("click", useCallback("remove-" + key, mkOnRemove(item.id))));
        // @formatter:on
    }

    private double computeTotal(List<Item> items) {
        var total = 0.0;
        for (var item : items) {
            total += item.price() * item.qty();
        }
        return total;
    }

    // :: State mapping helpers

    private List<Item> getItems(ViewScope scope) {
        if (scope == null)
            return List.of();
        var v = scope.getState().get("items");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Item>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m)
                    result.add(getItem(m));
            }
            return result;
        }
        return List.of();
    }

    private Item getItem(Map<?, ?> item) {
        var id = CoerceUtils.asLong(item.get("id"), Long.MIN_VALUE);
        var name = CoerceUtils.asString(item.get("name"));
        var price = CoerceUtils.asDouble(item.get("price"), 0.0);
        var qty = CoerceUtils.asInteger(item.get("quantity"), 1);
        return new Item(id, name, price, qty);
    }

}
