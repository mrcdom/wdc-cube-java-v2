package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.Collections;
import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class ReceiptViewVDom extends AbstractVDomView<ReceiptPresenter> {

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
                .prop("margin", "0 auto")
                .padding("20px")
                .build();

        String SUCCESS_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("14px 18px")
                .background("#f0fdf4")
                .border("1px solid #bbf7d0")
                .borderRadius("var(--app-radius-sm)")
                .marginBottom("16px")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String SUCCESS_ICON = css()
                .color("#16a34a")
                .fontSize("1.2rem")
                .flexShrink(0)
                .build();

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
                .prop("margin", "0")
                .fontWeight("700")
                .fontSize("1rem")
                .color("var(--app-text)")
                .build();

        String HEADER_SUBTITLE = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String RECEIPT_BODY = css()
                .background("var(--app-bg)")
                .border("1px solid var(--app-border)")
                .borderRadius("var(--app-radius-sm)")
                .padding("20px")
                .prop("font-family", "'Courier New',Courier,monospace")
                .fontSize("0.82rem")
                .build();

        String DATE_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .marginBottom("10px")
                .paddingBottom("10px")
                .prop("border-bottom", "1px dashed var(--app-border)")
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
                .prop("border-top", "2px solid var(--app-accent)")
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

        String TABLE_HEADER = css()
                .displayFlex()
                .fontWeight("700")
                .fontSize("0.75rem")
                .marginBottom("6px")
                .paddingBottom("6px")
                .prop("border-bottom", "1px solid var(--app-border)")
                .color("var(--app-text-secondary)")
                .prop("text-transform", "uppercase")
                .prop("letter-spacing", "0.5px")
                .build();

        String COL_ITEM = css()
                .flex("1")
                .build();

        String COL_QTY = css()
                .width("80px")
                .textAlign("center")
                .build();

        String COL_VALUE = css()
                .width("100px")
                .textAlign("right")
                .build();

        String ITEM_ROW = css()
                .displayFlex()
                .alignItems("center")
                .padding("6px 0")
                .prop("border-bottom", "1px dotted var(--app-border)")
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
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            // Success banner
            div().style(showSuccess ? Styles.SUCCESS_VISIBLE : Styles.HIDDEN).children(
              span("bi bi-check-circle-fill").style(Styles.SUCCESS_ICON),
              span().style(Styles.SUCCESS_TEXT).text("Compra realizada com sucesso!")),
            div().style(Styles.CARD).children(
              // Header
              div().style(Styles.HEADER_ROW).children(
                div().style(Styles.HEADER_ICON_BOX)
                  .children(span("bi bi-receipt").style(Styles.HEADER_ICON)),
                div().children(
                  h5().style(Styles.HEADER_TITLE).text("Recibo de Compra"),
                  span().style(Styles.HEADER_SUBTITLE).text("WDC Shopping"))),
              // Receipt content (monospace)
              div().style(Styles.RECEIPT_BODY).children(
                div().style(Styles.DATE_ROW).children(
                  span().style(Styles.DATE_LABEL).text("Data:"),
                  span().style(Styles.DATE_VALUE).text(dateText)),
                renderItemsTable(items),
                div().style(Styles.TOTAL_ROW).children(
                  span().style(Styles.TOTAL_LABEL).text("TOTAL:"),
                  span().style(Styles.TOTAL_VALUE).text(totalText))),
              // Back button
              spActionButton().style(Styles.BACK_BTN)
                .children(span("bi bi-arrow-left"), span().text(" Voltar aos produtos"))
                .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        // @formatter:off
        return div().children(
          div().style(Styles.TABLE_HEADER).children(
            span().style(Styles.COL_ITEM).text("ITEM"),
            span().style(Styles.COL_QTY).text("QTD"),
            span().style(Styles.COL_VALUE).text("VALOR")),
          div().children(items.stream().map(this::renderItemRow).toList()));
        // @formatter:on
    }

    private VNode renderItemRow(ReceiptItem item) {
        var desc = item.description != null ? item.description : "";
        var qty = String.valueOf(item.quantity);
        var value = "R$ " + String.format("%.2f", item.value);

        // @formatter:off
        return div().key(desc + qty).style(Styles.ITEM_ROW).children(
          span().style(Styles.ITEM_DESC).text(desc),
          span().style(Styles.ITEM_QTY).text(qty),
          span().style(Styles.ITEM_VALUE).text(value));
        // @formatter:on
    }
}
