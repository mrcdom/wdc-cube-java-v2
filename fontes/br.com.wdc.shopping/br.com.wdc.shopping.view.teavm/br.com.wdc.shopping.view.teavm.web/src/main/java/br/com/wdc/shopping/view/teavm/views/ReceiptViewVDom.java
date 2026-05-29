package br.com.wdc.shopping.view.teavm.views;

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
        return div("").style("flex:1;min-height:0;overflow-y:auto;background:var(--app-bg)").children(
                div("")
                        .style("max-width:900px;margin:0 auto;padding:20px")
                        .children(
                        // Success banner
                        div("")
                                .style(showSuccess ? "display:flex;align-items:center;gap:10px;padding:14px 18px;background:#f0fdf4;border:1px solid #bbf7d0;border-radius:var(--app-radius-sm);margin-bottom:16px" : "display:none")
                                .children(
                                        span("bi bi-check-circle-fill").style("color:#16a34a;font-size:1.2rem;flex-shrink:0"),
                                        span("").style("font-size:0.9rem;color:#166534;font-weight:600").text("Compra realizada com sucesso!")),

                        div("")
                                .style("background:var(--app-surface);border-radius:var(--app-radius);border:1px solid var(--app-border);padding:28px;box-shadow:var(--app-shadow-sm)")
                                .children(
                                        // Header
                                        div("").style("display:flex;align-items:center;gap:10px;margin-bottom:20px").children(
                                                div("")
                                                        .style("width:40px;height:40px;background:var(--app-accent-light);border-radius:10px;display:flex;align-items:center;justify-content:center")
                                                        .children(span("bi bi-receipt").style("color:var(--app-accent);font-size:1.1rem")),
                                                div("").children(
                                                        h5("").style("margin:0;font-weight:700;font-size:1rem;color:var(--app-text)").text("Recibo de Compra"),
                                                        span("").style("font-size:0.75rem;color:var(--app-text-secondary)").text("WDC Shopping"))),

                                        // Receipt content (monospace)
                                        div("")
                                                .style("background:var(--app-bg);border:1px solid var(--app-border);border-radius:var(--app-radius-sm);padding:20px;font-family:'Courier New',Courier,monospace;font-size:0.82rem")
                                                .children(
                                                        // Date row
                                                        div("").style("display:flex;justify-content:space-between;margin-bottom:10px;padding-bottom:10px;border-bottom:1px dashed var(--app-border)").children(
                                                                span("").style("color:var(--app-text-secondary)").text("Data:"),
                                                                span("").style("font-weight:600;color:var(--app-text)").text(dateText)),

                                                        // Items table
                                                        renderItemsTable(items),

                                                        // Total row
                                                        div("").style("display:flex;justify-content:space-between;margin-top:12px;padding-top:12px;border-top:2px solid var(--app-accent)").children(
                                                                span("").style("font-weight:700;color:var(--app-text)").text("TOTAL:"),
                                                                span("")
                                                                        .style("font-size:1.1rem;font-weight:800;color:var(--app-accent)")
                                                                        .text(totalText))),

                                        // Back button
                                        spActionButton()
                                                .style("margin-top:20px")
                                                .children(
                                                        span("bi bi-arrow-left"),
                                                        span("").text(" Voltar aos produtos"))
                                                .on("click",
                                                        evt -> safeAction("Back", this.presenter::onOpenProducts)))));
        // @formatter:on
    }

    private VNode renderItemsTable(List<ReceiptItem> items) {
        // @formatter:off
        return div("")
                .children(
                        // Header
                        div("")
                                .style("display:flex;font-weight:700;font-size:0.75rem;margin-bottom:6px;padding-bottom:6px;border-bottom:1px solid var(--app-border);color:var(--app-text-secondary);text-transform:uppercase;letter-spacing:0.5px")
                                .children(
                                        span("").style("flex:1").text("ITEM"),
                                        span("").style("width:80px;text-align:center").text("QTD"),
                                        span("").style("width:100px;text-align:right").text("VALOR")),
                        // Items
                        div("").children(items.stream().map(this::renderItemRow).toList()));
        // @formatter:on
    }

    private VNode renderItemRow(ReceiptItem item) {
        var desc = item.description != null ? item.description : "";
        var qty = String.valueOf(item.quantity);
        var value = "R$ " + String.format("%.2f", item.value);

        // @formatter:off
        return div("")
                .key(desc + qty)
                .style("display:flex;align-items:center;padding:6px 0;border-bottom:1px dotted var(--app-border)")
                .children(
                        span("").style("flex:1;font-size:0.82rem;color:var(--app-text)").text(desc),
                        span("").style("width:80px;text-align:center;font-size:0.82rem;color:var(--app-text-secondary)").text(qty),
                        span("").style("width:100px;text-align:right;font-weight:700;font-size:0.82rem;color:var(--app-text)").text(value));
        // @formatter:on
    }
}
