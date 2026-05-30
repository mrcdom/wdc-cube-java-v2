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
 * Receipt view.
 * State: receipt {items [{description, quantity, value}], date, total}, notifySuccess.
 */
public class ReceiptView extends AbstractRemoteView {

    public static final String VIEW_ID = "e8d0bd8ae3bc";

    private static final int ON_BACK = 1;

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

        String SUCCESS_BANNER = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("14px 18px")
                .background("#f0fdf4")
                .border("1px solid #bbf7d0")
                .borderRadius("var(--app-radius-sm)")
                .marginBottom("16px")
                .build();

        String HIDDEN = css().displayNone().build();

        String SUCCESS_ICON = css()
                .color("#16a34a")
                .fontSize("1.2rem")
                .flexShrink(0).build();

        String SUCCESS_TEXT = css()
                .fontSize("0.9rem")
                .color("#166534")
                .fontWeight("600")
                .build();

        String CARD = css()
                .background("var(--app-surface)")
                .borderRadius("var(--app-radius)")
                .border("1px solid var(--app-border)")
                .padding("28px")
                .boxShadow("var(--app-shadow-sm)")
                .build();

        String HEADER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .marginBottom("20px")
                .build();

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
                .fontSize("1rem")
                .color("var(--app-text)")
                .build();

        String HEADER_SUBTITLE = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String RECEIPT_BOX = css()
                .background("var(--app-bg)")
                .border("1px solid var(--app-border)")
                .borderRadius("var(--app-radius-sm)")
                .padding("20px")
                .fontFamily("'Courier New',Courier,monospace")
                .fontSize("0.82rem")
                .build();

        String DATE_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .marginBottom("10px")
                .paddingBottom("10px")
                .borderBottom("1px dashed var(--app-border)")
                .build();

        String DATE_LABEL = css()
                .color("var(--app-text-secondary)")
                .build();

        String DATE_VALUE = css()
                .fontWeight("600")
                .color("var(--app-text)")
                .build();

        String TOTAL_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .marginTop("12px")
                .paddingTop("12px")
                .borderTop("2px solid var(--app-accent)")
                .build();

        String TOTAL_LABEL = css()
                .fontWeight("700")
                .color("var(--app-text)")
                .build();

        String TOTAL_VALUE = css()
                .fontSize("1.1rem")
                .fontWeight("800")
                .color("var(--app-accent)")
                .build();

        String BACK_BTN = css()
                .marginTop("20px")
                .build();

        // Table header
        String TABLE_HEADER = css()
                .displayFlex()
                .fontWeight("700")
                .fontSize("0.75rem")
                .marginBottom("6px")
                .paddingBottom("6px")
                .borderBottom("1px solid var(--app-border)")
                .color("var(--app-text-secondary)")
                .prop("text-transform", "uppercase")
                .prop("letter-spacing", "0.5px")
                .build();

        String COL_ITEM = css().flex("1")
                .build();

        String COL_QTY = css().width("80px")
                .textAlign("center").build();

        String COL_VALUE = css().width("100px")
                .textAlign("right")
                .build();

        // Item row
        String ITEM_ROW = css()
                .displayFlex()
                .alignItems("center")
                .padding("6px 0")
                .borderBottom("1px dotted var(--app-border)")
                .build();

        String ITEM_DESC = css()
                .flex("1")
                .fontSize("0.82rem")
                .color("var(--app-text)")
                .build();

        String ITEM_QTY = css()
                .width("80px")
                .textAlign("center")
                .fontSize("0.82rem")
                .color("var(--app-text-secondary)")
                .build();

        String ITEM_VALUE = css()
                .width("100px")
                .textAlign("right")
                .fontWeight("700")
                .fontSize("0.82rem")
                .color("var(--app-text)")
                .build();
    }

    public ReceiptView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        boolean showSuccess = scope.getBoolean("notifySuccess");

        Map<String, Object> receipt = scope.getMap("receipt");
        List<Map<String, Object>> items = List.of();
        String dateText = "";
        String totalText = "";

        if (!receipt.isEmpty()) {
            items = getReceiptItems(receipt);
            var date = receipt.get("date");
            dateText = date != null ? date.toString() : "";
            var total = receipt.get("total");
            totalText = total instanceof Number n ? "R$ " + String.format("%.2f", n.doubleValue()) : "";
        }

        final List<Map<String, Object>> finalItems = items;

        // @formatter:off
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            // Success banner
            div().style(showSuccess ? Styles.SUCCESS_BANNER : Styles.HIDDEN).children(
              span("bi bi-check-circle-fill").style(Styles.SUCCESS_ICON),
              span().style(Styles.SUCCESS_TEXT).text("Compra realizada com sucesso!")),
            div().style(Styles.CARD).children(
              div().style(Styles.HEADER_ROW).children(
                div().style(Styles.HEADER_ICON_BOX).children(
                  span("bi bi-receipt").style(Styles.HEADER_ICON)),
                div().children(
                  h5().style(Styles.HEADER_TITLE).text("Recibo de Compra"),
                  span().style(Styles.HEADER_SUBTITLE).text("WDC Shopping"))),
              div().style(Styles.RECEIPT_BOX).children(
                div().style(Styles.DATE_ROW).children(
                  span().style(Styles.DATE_LABEL).text("Data:"),
                  span().style(Styles.DATE_VALUE).text(dateText)),
                renderItemsTable(finalItems),
                div().style(Styles.TOTAL_ROW).children(
                  span().style(Styles.TOTAL_LABEL).text("TOTAL:"),
                  span().style(Styles.TOTAL_VALUE).text(totalText))),
              spActionButton()
                .style(Styles.BACK_BTN)
                .children(span("bi bi-arrow-left"), span().text(" Voltar aos produtos"))
                .on("click", evt -> submit(ON_BACK)))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<Map<String, Object>> items) {
        // @formatter:off
        return div().children(
          div().style(Styles.TABLE_HEADER).children(
            span().style(Styles.COL_ITEM).text("ITEM"),
            span().style(Styles.COL_QTY).text("QTD"),
            span().style(Styles.COL_VALUE).text("VALOR")),
          div().children(items.stream().map(this::renderItemRow).toList()));
        // @formatter:on
    }

    private VNode renderItemRow(Map<String, Object> item) {
        var desc = item.get("description") != null ? item.get("description").toString() : "";
        var qtyVal = item.get("quantity");
        var qty = qtyVal instanceof Number n ? String.valueOf(n.intValue()) : "1";
        var valObj = item.get("value");
        var value = valObj instanceof Number n ? "R$ " + String.format("%.2f", n.doubleValue()) : "";

        // @formatter:off
        return div().key(desc + qty).style(Styles.ITEM_ROW).children(
          span().style(Styles.ITEM_DESC).text(desc),
          span().style(Styles.ITEM_QTY).text(qty),
          span().style(Styles.ITEM_VALUE).text(value));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getReceiptItems(Map<String, Object> receipt) {
        var v = receipt.get("items");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Map<String, Object>>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
    }
}
