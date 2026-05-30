package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;

public class ReceiptViewVDom extends AbstractVDomView<ReceiptPresenter> {

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

    private final ReceiptViewState state;

    public ReceiptViewVDom(ReceiptPresenter presenter) {
        super("receipt", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Flag one-shot: true apenas no render imediato após a compra
        final boolean showSuccess = this.state.notifySuccess;
        if (this.state.notifySuccess) {
            this.state.notifySuccess = false;
        }

        var items = Collections.<ReceiptItem>emptyList();
        var dateText = "";
        var totalText = "";

        if (this.state.receipt != null) {
            items = this.state.receipt.items != null ? this.state.receipt.items : Collections.emptyList();
            if (this.state.receipt.date != null) {
                dateText = DateUtils.formatDateTime(this.state.receipt.date);
            }
            var total = this.state.receipt.total != null ? this.state.receipt.total : 0.0;
            totalText = "R$ " + String.format("%.2f", total);
        }

        // @formatter:off
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            // Success banner
            div(showSuccess ? Css.SUCCESS_VISIBLE : Css.HIDDEN).children(
              span(Css.SUCCESS_ICON),
              span(Css.SUCCESS_TEXT).text("Compra realizada com sucesso!")),
            div(Css.CARD).children(
              // Header
              div(Css.HEADER_ROW).children(
                div(Css.HEADER_ICON_BOX)
                  .children(span(Css.HEADER_ICON)),
                div().children(
                  h5(Css.HEADER_TITLE).text("Recibo de Compra"),
                  span(Css.HEADER_SUBTITLE).text("WDC Shopping"))),
              // Receipt content (monospace)
              div(Css.RECEIPT_BODY).children(
                div(Css.DATE_ROW).children(
                  span(Css.DATE_LABEL).text("Data:"),
                  span(Css.DATE_VALUE).text(dateText)),
                renderItemsTable(items),
                div(Css.TOTAL_ROW).children(
                  span(Css.TOTAL_LABEL).text("TOTAL:"),
                  span(Css.TOTAL_VALUE).text(totalText))),
              // Back button
              spActionButton().cls(Css.BACK_BTN)
                .children(span(CssIcons.ARROW_LEFT), span().text(" Voltar aos produtos"))
                .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        // @formatter:off
        return div().children(
          div(Css.TABLE_HEADER).children(
            span(Css.COL_ITEM).text("ITEM"),
            span(Css.COL_QTY).text("QTD"),
            span(Css.COL_VALUE).text("VALOR")),
          div().children(items.stream().map(this::renderItemRow).toList()));
        // @formatter:on
    }

    private VNode renderItemRow(ReceiptItem item) {
        var desc = item.description != null ? item.description : "";
        var qty = String.valueOf(item.quantity);
        var value = "R$ " + String.format("%.2f", item.value);

        // @formatter:off
        return div(Css.ITEM_ROW).key(desc + qty).children(
          span(Css.ITEM_DESC).text(desc),
          span(Css.ITEM_QTY).text(qty),
          span(Css.ITEM_VALUE).text(value));
        // @formatter:on
    }
}
