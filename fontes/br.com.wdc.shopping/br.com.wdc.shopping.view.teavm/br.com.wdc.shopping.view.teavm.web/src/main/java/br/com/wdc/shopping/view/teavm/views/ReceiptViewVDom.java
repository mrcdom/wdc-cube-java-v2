package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import java.util.Collections;
import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.util.DateUtils;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class ReceiptViewVDom extends AbstractVDomView<ReceiptPresenter> {

    private final ReceiptViewState state;

    public ReceiptViewVDom(ReceiptPresenter presenter) {
        super("receipt", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
        this.element.setAttribute("style", "flex:1;min-height:0;overflow-y:auto");
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

        return div("")
                .style(PAGE_WRAPPER)
                .children(
                        div("")
                                .style(CARD_LARGE)
                                .children(
                                        h5("")
                                                .style(SECTION_LABEL + ";margin:0 0 16px 0")
                                                .text("IMPRIMA SEU RECIBO:"),

                                        // Success banner
                                        div(showSuccess
                                                        ? "d-flex align-items-center gap-2 mb-3"
                                                        : "d-none")
                                                .style(showSuccess ? SUCCESS_BANNER : "")
                                                .children(
                                                        span(BsIcons.CHECK_CIRCLE),
                                                        span("")
                                                                .style(SUCCESS_TEXT_STYLE)
                                                                .text("Compra realizada com sucesso!")),

                                        // Receipt content (monospace)
                                        div("")
                                                .style(RECEIPT_BOX)
                                                .children(
                                                        span("")
                                                                .style("font-weight:600;display:block;margin-bottom:4px")
                                                                .text("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET"),
                                                        span("")
                                                                .style("color:" + TEXT_SECONDARY + ";display:block;margin-bottom:12px")
                                                                .text("Recibo de compra"),

                                                        // Date row
                                                        div("d-flex justify-content-between mb-2").children(
                                                                span("").style("color:" + TEXT_SECONDARY).text("Data:"),
                                                                span("fw-bold").text(dateText)),

                                                        // Total row
                                                        div("d-flex justify-content-between mb-3").children(
                                                                span("").style("color:" + TEXT_SECONDARY).text("Total:"),
                                                                span("")
                                                                        .style(PRICE_MD)
                                                                        .text(totalText)),

                                                        // Items table
                                                        renderItemsTable(items)),

                                        // Back button
                                        button("btn btn-link mt-3 p-0")
                                                .style(BTN_LINK)
                                                .children(
                                                        span(BsIcons.ARROW_BACK),
                                                        span("").text(" Voltar aos produtos"))
                                                .on("click",
                                                        evt -> safeAction("Back", this.presenter::onOpenProducts))));
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        return div("")
                .style(SEPARATOR + ";padding-top:8px")
                .children(
                        // Header
                        div("d-flex fw-bold small mb-1 pb-1")
                                .style(SEPARATOR + ";color:" + TEXT_SECONDARY)
                                .children(
                                        span("").style("flex:1").text("ITEM"),
                                        span("").style("width:80px;text-align:center").text("QTD"),
                                        span("").style("width:100px;text-align:right").text("VALOR")),
                        // Items
                        div("").children(items.stream().map(this::renderItemRow).toList()));
    }

    private VNode renderItemRow(ReceiptItem item) {
        var desc = item.description != null ? item.description : "";
        var qty = String.valueOf(item.quantity);
        var value = "R$ " + String.format("%.2f", item.value);

        return div("")
                .key(desc + qty)
                .style("display:flex;align-items:center;padding:6px 0")
                .children(
                        span("").style("flex:1;font-size:0.85rem").text(desc),
                        span("").style("width:80px;text-align:center;font-size:0.85rem").text(qty),
                        span("").style("width:100px;text-align:right;font-weight:bold;font-size:0.85rem").text(value));
    }
}
