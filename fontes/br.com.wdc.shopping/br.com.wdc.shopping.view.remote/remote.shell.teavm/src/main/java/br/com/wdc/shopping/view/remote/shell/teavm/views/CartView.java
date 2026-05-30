package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.vdom.VNode;

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
    private interface Styles {

        String ROOT = css()
                .flex("1")
                .minHeight("0")
                .overflowY("auto")
                .background("var(--app-bg)")
                .build();

        String WRAPPER = css()
                .maxWidth("900px")
                .margin("0 auto")
                .padding("20px")
                .build();

        String CARD = css()
                .background("var(--app-surface)")
                .borderRadius("var(--app-radius)")
                .border("1px solid var(--app-border)")
                .padding("24px")
                .boxShadow("var(--app-shadow-sm)")
                .build();

        String HEADER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .marginBottom("20px").build();

        String HEADER_ICON_BOX = css()
                .width("40px")
                .height("40px")
                .background("var(--app-accent-light)")
                .borderRadius("10px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .build();

        String HEADER_ICON = css()
                .color("var(--app-accent)")
                .fontSize("1.1rem")
                .build();

        String HEADER_TITLE = css()
                .margin("0")
                .fontWeight("700")
                .fontSize("1.1rem")
                .color("var(--app-text)")
                .build();

        String HEADER_SUBTITLE = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String ERROR_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("12px 16px")
                .background("#fef2f2")
                .border("1px solid #fecaca")
                .borderRadius("var(--app-radius-sm)")
                .marginBottom("12px")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String ERROR_ICON = css()
                .color("#dc2626")
                .fontSize("1rem")
                .flexShrink(0)
                .build();

        String ERROR_TEXT = css()
                .fontSize("0.85rem")
                .color("#991b1b")
                .fontWeight("500")
                .build();

        // Empty state
        String EMPTY_WRAP = css()
                .flexCol()
                .alignItems("center")
                .justifyContent("center")
                .padding("48px 0")
                .build();

        String EMPTY_CIRCLE = css()
                .width("100px")
                .height("100px")
                .background("var(--app-accent-light)")
                .borderRadius("50%")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .marginBottom("20px")
                .build();

        String EMPTY_ICON = css()
                .fontSize("2.5rem")
                .color("var(--app-accent)")
                .build();

        String EMPTY_TITLE = css()
                .color("var(--app-text)")
                .fontSize("1.1rem")
                .fontWeight("500")
                .margin("0 0 8px 0")
                .build();

        String EMPTY_SUBTITLE = css()
                .color("var(--app-text-secondary)")
                .fontSize("0.85rem")
                .margin("0 0 16px 0")
                .build();

        String ICON_MR = css()
                .marginRight("6px")
                .build();

        // Content
        String TOTAL_ROW = css()
                .displayFlex()
                .justifyContent("flex-end")
                .alignItems("center")
                .paddingTop("16px")
                .marginTop("16px")
                .borderTop("1px solid var(--app-border)")
                .build();

        String TOTAL_LABEL = css()
                .fontSize("0.85rem")
                .color("var(--app-text-secondary)")
                .build();

        String TOTAL_AMOUNT = css()
                .fontSize("1.4rem")
                .fontWeight("800")
                .color("var(--app-accent)")
                .marginLeft("8px")
                .build();

        String ACTIONS_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .alignItems("center")
                .marginTop("16px")
                .build();

        // Item row
        String ITEM_ROW = css()
                .displayFlex()
                .alignItems("center")
                .padding("12px 0")
                .borderBottom("1px solid var(--app-border)")
                .gap("8px").build();

        String ITEM_NAME = css()
                .flex("1")
                .fontWeight("500")
                .fontSize("0.88rem")
                .color("var(--app-text)")
                .build();

        String STEPPER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("4px")
                .build();

        String STEPPER_ICON = css()
                .fontSize("0.7rem")
                .build();

        String QTY_TEXT = css()
                .fontWeight("700")
                .minWidth("24px")
                .textAlign("center")
                .fontSize("0.85rem")
                .color("var(--app-text)")
                .build();

        String SUBTOTAL = css()
                .width("90px")
                .textAlign("right")
                .fontWeight("700")
                .color("var(--app-accent)")
                .fontSize("0.85rem")
                .build();

        String REMOVE_ICON = css()
                .fontSize("0.7rem")
                .color("#dc3545")
                .build();
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
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            div().style(Styles.CARD).children(
              div().style(Styles.HEADER_ROW).children(
                div().style(Styles.HEADER_ICON_BOX).children(
                  span("bi bi-bag").style(Styles.HEADER_ICON)),
                div().children(
                  h5().style(Styles.HEADER_TITLE).text("Carrinho"),
                  span().style(Styles.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
                span("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
                span().style(Styles.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
              // Empty state
              renderEmptyState(empty),
              // Content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div().style(empty ? Styles.EMPTY_WRAP : Styles.HIDDEN).children(
          div().style(Styles.EMPTY_CIRCLE).children(
            span("bi bi-bag").style(Styles.EMPTY_ICON)),
          p().style(Styles.EMPTY_TITLE).text("Carrinho vazio"),
          p().style(Styles.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span("bi bi-grid-3x3-gap").style(Styles.ICON_MR), span().text("Ver produtos"))
            .on("click", evt -> submit(ON_BACK)));
        // @formatter:on
    }

    private VNode renderContent(List<Map<String, Object>> items, boolean empty, String totalText) {
        // @formatter:off
        return div().style(empty ? Styles.HIDDEN : "").children(
          div().children(items.stream().map(this::renderItem).toList()),
          div().style(Styles.TOTAL_ROW).children(
            span().style(Styles.TOTAL_LABEL).text("Total: "),
            span().style(Styles.TOTAL_AMOUNT).text(totalText)),
          div().style(Styles.ACTIONS_ROW).children(
            spActionButton()
              .children(span("bi bi-arrow-left"), span().text(" Continuar comprando"))
              .on("click", evt -> submit(ON_BACK)),
            spButton("accent", "l")
              .children(span("bi bi-check2-circle").style(Styles.ICON_MR), span().text("Finalizar pedido"))
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
        return div().key(key).style(Styles.ITEM_ROW).children(
          span().style(Styles.ITEM_NAME).text(name),
          // Quantity stepper
          div().style(Styles.STEPPER_ROW).children(
            spActionButton("s")
              .children(span("bi bi-dash").style(Styles.STEPPER_ICON))
              .on("click", evt -> { setFormField("p.productId", id); setFormField("p.quantity", qty - 1); submit(ON_MODIFY_QUANTITY); }),
            span().style(Styles.QTY_TEXT).text(String.valueOf(qty)),
            spActionButton("s")
              .children(span("bi bi-plus").style(Styles.STEPPER_ICON))
              .on("click", evt -> { setFormField("p.productId", id); setFormField("p.quantity", qty + 1); submit(ON_MODIFY_QUANTITY); })),
          span().style(Styles.SUBTOTAL).text(subtotal),
          // Remove button
          spActionButton("s")
            .children(span("bi bi-x-lg").style(Styles.REMOVE_ICON))
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
