package br.com.wdc.shopping.view.remote.shell.teavm.views.receipt;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.h5;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Receipt view. State: receipt {items [{description, quantity, value}], date, total}, notifySuccess.
 */
public class ReceiptView extends AbstractRemoteView {

    public static final String VIEW_ID = "e8d0bd8ae3bc";

    private static final int ON_BACK = 1;

    @SuppressWarnings({ "java:S1214", "static-access" })
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String SUCCESS_VISIBLE = c.ALERT_SUCCESS;
        String HIDDEN = u.HIDDEN;
        String SUCCESS_ICON = clsx(icon.CHECK_CIRCLE_FILL, c.ALERT_SUCCESS_ICON);
        String SUCCESS_TEXT = c.ALERT_SUCCESS_TEXT;
        String CARD = c.CARD_PANEL_LG;
        String HEADER_ROW = c.CARD_HEADER_ROW;
        String HEADER_ICON_BOX = c.CARD_HEADER_ICON_BOX;
        String HEADER_ICON = clsx(icon.RECEIPT, c.CARD_HEADER_ICON);
        String HEADER_TITLE = "receipt-header-title";
        String HEADER_SUBTITLE = c.CARD_HEADER_SUBTITLE;
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

    // Stable event listener
    private final EventListener<Event> onBack = evt -> submit(ON_BACK);

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
            dateText = formatDateTime(receipt.get("date"));
            var total = receipt.get("total");
            totalText = total instanceof Number n ? "R$ " + String.format("%.2f", n.doubleValue()) : "";
        }

        final List<Map<String, Object>> finalItems = items;

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            // Success banner
            div(showSuccess ? Sel.SUCCESS_VISIBLE : Sel.HIDDEN).children(
              span(Sel.SUCCESS_ICON),
              span(Sel.SUCCESS_TEXT).text("Compra realizada com sucesso!")),
            div(Sel.CARD).children(
              div(Sel.HEADER_ROW).children(
                div(Sel.HEADER_ICON_BOX).children(
                  span(Sel.HEADER_ICON)),
                div().children(
                  h5(Sel.HEADER_TITLE).text("Recibo de Compra"),
                  span(Sel.HEADER_SUBTITLE).text("WDC Shopping"))),
              div(Sel.RECEIPT_BODY).children(
                div(Sel.DATE_ROW).children(
                  span(Sel.DATE_LABEL).text("Data:"),
                  span(Sel.DATE_VALUE).text(dateText)),
                renderItemsTable(finalItems),
                div(Sel.TOTAL_ROW).children(
                  span(Sel.TOTAL_LABEL).text("TOTAL:"),
                  span(Sel.TOTAL_VALUE).text(totalText))),
              spActionButton()
                .cls(Sel.BACK_BTN)
                .children(span(SelIcons.ARROW_LEFT), span().text(" Voltar aos produtos"))
                .on("click", onBack))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<Map<String, Object>> items) {
        // @formatter:off
        return div().children(
          div(Sel.TABLE_HEADER).children(
            span(Sel.COL_ITEM).text("ITEM"),
            span(Sel.COL_QTY).text("QTD"),
            span(Sel.COL_VALUE).text("VALOR")),
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
        return div(Sel.ITEM_ROW).key(desc + qty).children(
          span(Sel.ITEM_DESC).text(desc),
          span(Sel.ITEM_QTY).text(qty),
          span(Sel.ITEM_VALUE).text(value));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getReceiptItems(Map<String, Object> receipt) {
        var v = receipt.get("items");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Map<String, Object>>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m)
                    result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
    }

    @SuppressWarnings("deprecation")
    private static String formatDateTime(Object dateObj) {
        if (!(dateObj instanceof Number n))
            return "";
        long millis = n.longValue();
        if (millis <= 0)
            return "";
        var d = new Date(millis);
        int day = d.getDate();
        int month = d.getMonth() + 1;
        int year = d.getYear() + 1900;
        int hour = d.getHours();
        int min = d.getMinutes();
        return (day < 10 ? "0" : "") + day + "/" + (month < 10 ? "0" : "") + month + "/" + year
                + " " + (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min;
    }
}
