package br.com.wdc.shopping.view.teavm.views;

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
    private boolean showSuccess;

    public ReceiptViewVDom(ReceiptPresenter presenter) {
        super("receipt", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir flag one-shot
        if (this.state.notifySuccess) {
            this.showSuccess = true;
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
                .style("max-width:900px;margin:0 auto;padding:12px")
                .children(
                        div("")
                                .style("background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:24px")
                                .children(
                                        h5("")
                                                .style("color:#666;font-size:0.85rem;margin:0 0 16px 0")
                                                .text("IMPRIMA SEU RECIBO:"),

                                        // Success banner
                                        div("d-flex align-items-center gap-2 mb-3")
                                                .style(this.showSuccess
                                                        ? "background-color:#e8f5e9;border:1px solid #a5d6a7;border-radius:8px;"
                                                                + "padding:12px 16px;color:#2e7d32;font-size:1.2rem"
                                                        : "display:none")
                                                .children(
                                                        span(BsIcons.CHECK_CIRCLE),
                                                        span("")
                                                                .style("color:#2e7d32;font-weight:bold;font-size:1rem")
                                                                .text("Compra realizada com sucesso!")),

                                        // Receipt content (monospace)
                                        div("")
                                                .style("border:1px solid #bdbdbd;border-radius:8px;padding:16px;"
                                                        + "font-family:'Courier New',Courier,monospace;font-size:0.85rem")
                                                .children(
                                                        span("")
                                                                .style("font-weight:600;display:block;margin-bottom:4px")
                                                                .text("WEDOCODE SHOPPING - SUA COMPRA CERTA NA INTERNET"),
                                                        span("")
                                                                .style("color:#666;display:block;margin-bottom:12px")
                                                                .text("Recibo de compra"),

                                                        // Date row
                                                        div("d-flex justify-content-between mb-2").children(
                                                                span("").style("color:#666").text("Data:"),
                                                                span("fw-bold").text(dateText)),

                                                        // Total row
                                                        div("d-flex justify-content-between mb-3").children(
                                                                span("").style("color:#666").text("Total:"),
                                                                span("")
                                                                        .style("font-weight:bold;color:#1976d2;font-size:1.1rem")
                                                                        .text(totalText)),

                                                        // Items table
                                                        renderItemsTable(items)),

                                        // Back button
                                        button("btn btn-link mt-3 p-0")
                                                .style("color:#1976d2;text-decoration:underline;font-size:0.85rem")
                                                .children(
                                                        span(BsIcons.ARROW_BACK),
                                                        span("").text(" Voltar aos produtos"))
                                                .on("click",
                                                        evt -> safeAction("Back", this.presenter::onOpenProducts))));
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        return div("")
                .style("border-top:1px solid #e0e0e0;padding-top:8px")
                .children(
                        // Header
                        div("d-flex fw-bold small mb-1 pb-1")
                                .style("border-bottom:1px solid #e0e0e0;color:#666")
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
