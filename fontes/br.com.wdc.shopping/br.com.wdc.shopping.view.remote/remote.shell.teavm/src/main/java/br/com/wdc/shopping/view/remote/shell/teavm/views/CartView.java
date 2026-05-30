package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.p;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Cart view.
 * State: items (array of {id, name, price, quantity}), errorMessage.
 */
public class CartView extends AbstractRemoteView {

    public static final String VIEW_ID = "7eb485e5f843";

    private static final int ON_BUY = 1;
    private static final int ON_REMOVE = 2;
    private static final int ON_BACK = 3;
    private static final int ON_MODIFY_QUANTITY = 4;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = CssUtility.PAGE_SCROLL_ROOT;
        String WRAPPER = CssUtility.PAGE_WRAPPER;
        String CARD = CssComponents.CARD_PANEL;
        String HEADER_ROW = CssComponents.CARD_HEADER_ROW;
        String HEADER_ICON_BOX = CssComponents.CARD_HEADER_ICON_BOX;
        String HEADER_ICON = clsx(CssIcons.BAG, CssComponents.CARD_HEADER_ICON);
        String HEADER_TITLE = CssComponents.CARD_HEADER_TITLE;
        String HEADER_SUBTITLE = CssComponents.CARD_HEADER_SUBTITLE;
        String ERROR_VISIBLE = clsx(CssComponents.ALERT_ERROR, CssUtility.MB_12);
        String HIDDEN = CssUtility.HIDDEN;
        String ERROR_ICON = clsx(CssIcons.EXCLAMATION_CIRCLE, CssComponents.ALERT_ERROR_ICON);
        String ERROR_TEXT = CssComponents.ALERT_ERROR_TEXT;
        String EMPTY_STATE = clsx(CssComponents.EMPTY_STATE, CssUtility.PY_48);
        String EMPTY_ICON_BOX = "cart-empty-icon-box";
        String EMPTY_ICON = clsx(CssIcons.BAG, "cart-empty-icon");
        String EMPTY_TITLE = "cart-empty-title";
        String EMPTY_SUBTITLE = "cart-empty-subtitle";
        String FOOTER = "cart-footer";
        String FOOTER_LABEL = "cart-footer-label";
        String FOOTER_TOTAL = "cart-footer-total";
        String ACTIONS_ROW = "cart-actions-row";
        String ITEM_ROW = "cart-item-row";
        String ITEM_NAME = "cart-item-name";
        String STEPPER_ROW = "cart-stepper-row";
        String STEPPER_MINUS_ICON = clsx(CssIcons.DASH, "cart-stepper-icon");
        String STEPPER_PLUS_ICON = clsx(CssIcons.PLUS, "cart-stepper-icon");
        String STEPPER_VALUE = "cart-stepper-value";
        String ITEM_SUBTOTAL = "cart-item-subtotal";
        String REMOVE_ICON = clsx(CssIcons.X_LG, "cart-remove-icon");
        String ICON_VIEW_PRODUCTS = clsx(CssIcons.GRID_3X3_GAP, CssUtility.MR_6);
        String ICON_FINALIZE = clsx(CssIcons.CHECK2_CIRCLE, CssUtility.MR_6);
    }

    public CartView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        List<Map<String, Object>> items = getItems();
        boolean empty = items.isEmpty();
        double totalCost = computeTotal(items);
        String totalText = "R$ " + String.format("%.2f", totalCost);

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            div(Css.CARD).children(
              div(Css.HEADER_ROW).children(
                div(Css.HEADER_ICON_BOX).children(
                  span(Css.HEADER_ICON)),
                div().children(
                  h5(Css.HEADER_TITLE).text("Carrinho"),
                  span(Css.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
                span(Css.ERROR_ICON),
                span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // Empty state
              renderEmptyState(empty),
              // Content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div(empty ? Css.EMPTY_STATE : Css.HIDDEN).children(
          div(Css.EMPTY_ICON_BOX).children(
            span(Css.EMPTY_ICON)),
          p(Css.EMPTY_TITLE).text("Carrinho vazio"),
          p(Css.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span(Css.ICON_VIEW_PRODUCTS), span().text("Ver produtos"))
            .on("click", evt -> submit(ON_BACK)));
        // @formatter:on
    }

    private VNode renderContent(List<Map<String, Object>> items, boolean empty, String totalText) {
        // @formatter:off
        return div(empty ? Css.HIDDEN : "").children(
          div().children(items.stream().map(this::renderItem).toList()),
          div(Css.FOOTER).children(
            span(Css.FOOTER_LABEL).text("Total: "),
            span(Css.FOOTER_TOTAL).text(totalText)),
          div(Css.ACTIONS_ROW).children(
            spActionButton()
              .children(span(CssIcons.ARROW_LEFT), span().text(" Continuar comprando"))
              .on("click", evt -> submit(ON_BACK)),
            spButton("accent", "l")
              .children(span(Css.ICON_FINALIZE), span().text("Finalizar pedido"))
              .on("click", evt -> submit(ON_BUY))));
        // @formatter:on
    }

    private VNode renderItem(Map<String, Object> item) {
        var id = item.get("id");
        var name = item.get("name") != null ? item.get("name").toString() : "";
        var priceVal = item.get("price");
        double price = priceVal instanceof Number n ? n.doubleValue() : 0;
        var qtyVal = item.get("quantity");
        int qty = qtyVal instanceof Number n ? n.intValue() : 1;
        var subtotal = "R$ " + String.format("%.2f", price * qty);
        var key = id != null ? id.toString() : name;

        // @formatter:off
        return div(Css.ITEM_ROW).key(key).children(
          span(Css.ITEM_NAME).text(name),
          // Quantity stepper
          div(Css.STEPPER_ROW).children(
            spActionButton("s")
              .children(span(Css.STEPPER_MINUS_ICON))
              .on("click", evt -> { setFormField("p.productId", id); setFormField("p.quantity", qty - 1); submit(ON_MODIFY_QUANTITY); }),
            span(Css.STEPPER_VALUE).text(String.valueOf(qty)),
            spActionButton("s")
              .children(span(Css.STEPPER_PLUS_ICON))
              .on("click", evt -> { setFormField("p.productId", id); setFormField("p.quantity", qty + 1); submit(ON_MODIFY_QUANTITY); })),
          span(Css.ITEM_SUBTOTAL).text(subtotal),
          // Remove button
          spActionButton("s")
            .children(span(Css.REMOVE_ICON))
            .on("click", evt -> { setFormField("p.productId", id); submit(ON_REMOVE); }));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getItems() {
        var scope = state();
        if (scope == null) return List.of();
        var v = scope.getState().get("items");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Map<String, Object>>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
    }

    private double computeTotal(List<Map<String, Object>> items) {
        double total = 0;
        for (var item : items) {
            var p = item.get("price");
            var q = item.get("quantity");
            double price = p instanceof Number n ? n.doubleValue() : 0;
            int qty = q instanceof Number n ? n.intValue() : 1;
            total += price * qty;
        }
        return total;
    }
}
