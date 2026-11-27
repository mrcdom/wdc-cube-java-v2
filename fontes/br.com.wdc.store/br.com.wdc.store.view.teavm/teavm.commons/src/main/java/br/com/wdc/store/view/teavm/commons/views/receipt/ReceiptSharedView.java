package br.com.wdc.store.view.teavm.commons.views.receipt;

import static br.com.wdc.store.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.store.view.teavm.commons.VNode.clsx;
import static br.com.wdc.store.view.teavm.commons.VNode.div;
import static br.com.wdc.store.view.teavm.commons.VNode.h5;
import static br.com.wdc.store.view.teavm.commons.VNode.span;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.store.i18n.StoreMessages;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.store.view.teavm.commons.DateUtils;
import br.com.wdc.store.view.teavm.commons.I18n;
import br.com.wdc.store.view.teavm.commons.SelComponents;
import br.com.wdc.store.view.teavm.commons.SelIcons;
import br.com.wdc.store.view.teavm.commons.SelUtility;
import br.com.wdc.store.view.teavm.commons.SharedVDomView;
import br.com.wdc.store.view.teavm.commons.VNode;

/**
 * Shared Receipt view.
 */
public class ReceiptSharedView extends SharedVDomView {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String ROOT = u.PAGE_SCROLL_ROOT;
        String WRAPPER = u.PAGE_WRAPPER;
        String HIDDEN = u.HIDDEN;
        String SUCCESS_VISIBLE = c.ALERT_SUCCESS;
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

    // -- External bindings --

    public Supplier<ReceiptScope> scopeSupplier;
    public Runnable onBack;

    // -- Stable event listener --

    private final EventListener<Event> backListener = evt -> onBack.run();

    // -- Render --

    @Override
    public VNode render() {
        var scope = scopeSupplier.get();
        boolean showSuccess = scope.notifySuccess.get();

        var items = Collections.<ReceiptItem>emptyList();
        var dateText = "";
        var totalText = "";

        if (scope.receipt.get() != null) {
            items = scope.receipt.get().items != null ? scope.receipt.get().items : Collections.emptyList();
            if (scope.receipt.get().date != null && scope.receipt.get().date > 0) {
                dateText = DateUtils.formatDateTime(scope.receipt.get().date);
            } else {
                dateText = I18n.text(StoreMessages.FORMAT_DATE_UNAVAILABLE);
            }
            var total = scope.receipt.get().total != null ? scope.receipt.get().total : 0.0;
            totalText = I18n.price(total);
        }

        // @formatter:off
        return div(Sel.ROOT).children(
          div(Sel.WRAPPER).children(
            // Success banner
            div(showSuccess ? Sel.SUCCESS_VISIBLE : Sel.HIDDEN).children(
              span(Sel.SUCCESS_ICON),
              span(Sel.SUCCESS_TEXT).text(I18n.text(StoreMessages.RECEIPT_COMPLETED))),
            div(Sel.CARD).children(
              // Header
              div(Sel.HEADER_ROW).children(
                div(Sel.HEADER_ICON_BOX)
                  .children(span(Sel.HEADER_ICON)),
                div().children(
                  h5(Sel.HEADER_TITLE).text(I18n.text(StoreMessages.RECEIPT_TITLE)),
                  span(Sel.HEADER_SUBTITLE).text(I18n.text(StoreMessages.APP_NAME)))),
              // Receipt content
              div(Sel.RECEIPT_BODY).children(
                div(Sel.DATE_ROW).children(
                  span(Sel.DATE_LABEL).text(I18n.text(StoreMessages.RECEIPT_DATE)),
                  span(Sel.DATE_VALUE).text(dateText)),
                renderItemsTable(items),
                div(Sel.TOTAL_ROW).children(
                  span(Sel.TOTAL_LABEL).text(I18n.text(StoreMessages.RECEIPT_TOTAL)),
                  span(Sel.TOTAL_VALUE).text(totalText))),
              // Back button
              spActionButton().cls(Sel.BACK_BTN)
                .children(span(SelIcons.ARROW_LEFT), span().text(" " + I18n.text(StoreMessages.RECEIPT_BACK_TO_PRODUCTS)))
                .on("click", backListener))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        // @formatter:off
        return div().children(
          div(Sel.TABLE_HEADER).children(
            span(Sel.COL_ITEM).text(I18n.text(StoreMessages.RECEIPT_ITEM)),
            span(Sel.COL_QTY).text(I18n.text(StoreMessages.RECEIPT_QUANTITY)),
            span(Sel.COL_VALUE).text(I18n.text(StoreMessages.RECEIPT_PRICE))),
          div().children(items.stream().map(this::renderItemRow).toList()));
        // @formatter:on
    }

    private VNode renderItemRow(ReceiptItem item) {
        var desc = item.description != null ? item.description : "";
        var qty = String.valueOf(item.quantity);
        var value = I18n.price(item.value);

        // @formatter:off
        return div(Sel.ITEM_ROW).key(desc + qty).children(
          span(Sel.ITEM_DESC).text(desc),
          span(Sel.ITEM_QTY).text(qty),
          span(Sel.ITEM_VALUE).text(value));
        // @formatter:on
    }
}
