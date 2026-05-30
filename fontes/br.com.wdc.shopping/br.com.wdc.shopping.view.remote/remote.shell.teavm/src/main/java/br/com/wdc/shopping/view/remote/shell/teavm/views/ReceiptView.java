package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
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
 * Receipt view.
 * State: receipt {items [{description, quantity, value}], date, total}, notifySuccess.
 */
public class ReceiptView extends AbstractRemoteView {

    public static final String VIEW_ID = "e8d0bd8ae3bc";

    private static final int ON_BACK = 1;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = CssUtility.PAGE_SCROLL_ROOT;
        String WRAPPER = CssUtility.PAGE_WRAPPER;
        String SUCCESS_VISIBLE = CssComponents.ALERT_SUCCESS;
        String HIDDEN = CssUtility.HIDDEN;
        String SUCCESS_ICON = clsx(CssIcons.CHECK_CIRCLE_FILL, CssComponents.ALERT_SUCCESS_ICON);
        String SUCCESS_TEXT = CssComponents.ALERT_SUCCESS_TEXT;
        String CARD = CssComponents.CARD_PANEL_LG;
        String HEADER_ROW = CssComponents.CARD_HEADER_ROW;
        String HEADER_ICON_BOX = CssComponents.CARD_HEADER_ICON_BOX;
        String HEADER_ICON = clsx(CssIcons.RECEIPT, CssComponents.CARD_HEADER_ICON);
        String HEADER_TITLE = "receipt-header-title";
        String HEADER_SUBTITLE = CssComponents.CARD_HEADER_SUBTITLE;
        String RECEIPT_BODY = "receipt-body";
        String DATE_ROW = "receipt-date-row";
        String DATE_LABEL = "receipt-date-label";
        String DATE_VALUE = "receipt-date-value";
        String TOTAL_ROW = "receipt-total-row";
        String TOTAL_LABEL = "receipt-total-label";
        String TOTAL_VALUE = "receipt-total-value";
        String BACK_BTN = "receipt-back-btn";
        String TABLE_HEADER = "receipt-table-header";
        String COL_ITEM = "receipt-col-item";
        String COL_QTY = "receipt-col-qty";
        String COL_VALUE = "receipt-col-value";
        String ITEM_ROW = "receipt-item-row";
        String ITEM_DESC = "receipt-item-desc";
        String ITEM_QTY = "receipt-item-qty";
        String ITEM_VALUE = "receipt-item-value";
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
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            // Success banner
            div(showSuccess ? Css.SUCCESS_VISIBLE : Css.HIDDEN).children(
              span(Css.SUCCESS_ICON),
              span(Css.SUCCESS_TEXT).text("Compra realizada com sucesso!")),
            div(Css.CARD).children(
              div(Css.HEADER_ROW).children(
                div(Css.HEADER_ICON_BOX).children(
                  span(Css.HEADER_ICON)),
                div().children(
                  h5(Css.HEADER_TITLE).text("Recibo de Compra"),
                  span(Css.HEADER_SUBTITLE).text("WDC Shopping"))),
              div(Css.RECEIPT_BODY).children(
                div(Css.DATE_ROW).children(
                  span(Css.DATE_LABEL).text("Data:"),
                  span(Css.DATE_VALUE).text(dateText)),
                renderItemsTable(finalItems),
                div(Css.TOTAL_ROW).children(
                  span(Css.TOTAL_LABEL).text("TOTAL:"),
                  span(Css.TOTAL_VALUE).text(totalText))),
              spActionButton()
                .cls(Css.BACK_BTN)
                .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar aos produtos"))
                .on("click", evt -> submit(ON_BACK)))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<Map<String, Object>> items) {
        // @formatter:off
        return div().children(
          div(Css.TABLE_HEADER).children(
            span(Css.COL_ITEM).text("ITEM"),
            span(Css.COL_QTY).text("QTD"),
            span(Css.COL_VALUE).text("VALOR")),
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
        return div(Css.ITEM_ROW).key(desc + qty).children(
          span(Css.ITEM_DESC).text(desc),
          span(Css.ITEM_QTY).text(qty),
          span(Css.ITEM_VALUE).text(value));
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
