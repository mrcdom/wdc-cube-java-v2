package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.Swc.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class CartViewVDom extends AbstractVDomView<CartPresenter> {

    private final CartViewState state;

    public CartViewVDom(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode != 0) {
            showError = true;
            errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            showError = false;
            errorMessage = "";
        }

        var items = this.state.items;
        var empty = items == null || items.isEmpty();
        var totalCost = computeTotalCost();
        var totalText = "R$ " + String.format("%.2f", totalCost);

        // @formatter:off
        return div("").style("flex:1;min-height:0;overflow-y:auto;background:var(--app-bg)").children(
                div("")
                        .style("max-width:900px;margin:0 auto;padding:20px")
                        .children(
                        div("")
                                .style("background:var(--app-surface);border-radius:var(--app-radius);border:1px solid var(--app-border);padding:24px;box-shadow:var(--app-shadow-sm)")
                                .children(
                                        // Title row
                                        div("").style("display:flex;align-items:center;gap:10px;margin-bottom:20px").children(
                                                div("")
                                                        .style("width:40px;height:40px;background:var(--app-accent-light);border-radius:10px;display:flex;align-items:center;justify-content:center")
                                                        .children(span("bi bi-bag").style("color:var(--app-accent);font-size:1.1rem")),
                                                div("").children(
                                                        h5("").style("margin:0;font-weight:700;font-size:1.1rem;color:var(--app-text)").text("Carrinho"),
                                                        span("").style("font-size:0.75rem;color:var(--app-text-secondary)").text("Seus produtos selecionados"))),

                                        // Error
                                        div("")
                                                .style(showError ? "display:flex;align-items:center;gap:10px;padding:12px 16px;background:#fef2f2;border:1px solid #fecaca;border-radius:var(--app-radius-sm);margin-bottom:12px" : "display:none")
                                                .children(
                                                        span("bi bi-exclamation-circle").style("color:#dc2626;font-size:1rem;flex-shrink:0"),
                                                        span("").style("font-size:0.85rem;color:#991b1b;font-weight:500").text(errorMessage)),

                                        // Empty cart state
                                        renderEmptyState(empty),

                                        // Cart content
                                        renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div("")
                .style(empty
                        ? "display:flex;flex-direction:column;align-items:center;justify-content:center;padding:48px 0"
                        : "display:none")
                .children(
                        div("")
                                .style("width:100px;height:100px;background:var(--app-accent-light);border-radius:50%;display:flex;align-items:center;justify-content:center;margin-bottom:20px")
                                .children(span("bi bi-bag").style("font-size:2.5rem;color:var(--app-accent)")),
                        p("").style("color:var(--app-text);font-size:1.1rem;font-weight:500;margin:0 0 8px 0").text("Carrinho vazio"),
                        p("").style("color:var(--app-text-secondary);font-size:0.85rem;margin:0 0 16px 0").text("Adicione produtos para começar"),
                        spButton("accent")
                                .children(span("bi bi-grid-3x3-gap").style("margin-right:6px"), span("").text("Ver produtos"))
                                .on("click", evt -> safeAction("Go shopping", this.presenter::onOpenProducts)));
        // @formatter:on
    }

    private VNode renderContent(List<CartItem> items, boolean empty, String totalText) {
        // @formatter:off
        return div("")
                .style(empty ? "display:none" : "")
                .children(
                        // Items list
                        div("").children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),

                        // Footer
                        div("")
                                .style("display:flex;justify-content:flex-end;align-items:center;padding-top:16px;margin-top:16px;border-top:1px solid var(--app-border)")
                                .children(
                                        span("").style("font-size:0.85rem;color:var(--app-text-secondary)").text("Total: "),
                                        span("")
                                                .style("font-size:1.4rem;font-weight:800;color:var(--app-accent);margin-left:8px")
                                                .text(totalText)),

                        // Actions
                        div("").style("display:flex;justify-content:space-between;align-items:center;margin-top:16px").children(
                                spActionButton()
                                        .children(
                                                span("bi bi-arrow-left"),
                                                span("").text(" Continuar comprando"))
                                        .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
                                spButton("accent", "l")
                                        .children(
                                                span("bi bi-check2-circle").style("margin-right:6px"),
                                                span("").text("Finalizar pedido"))
                                        .on("click", evt -> safeAction("Buy", this.presenter::onBuy))));
        // @formatter:on
    }

    private VNode renderItem(CartItem item) {
        var name = item.name != null ? item.name : "";
        var subtotal = "R$ " + String.format("%.2f", item.price * item.quantity);

        // @formatter:off
        return div("")
                .key(String.valueOf(item.id))
                .style("display:flex;align-items:center;padding:12px 0;border-bottom:1px solid var(--app-border);gap:8px")
                .children(
                        // Product name
                        span("").style("flex:1;font-weight:500;font-size:0.88rem;color:var(--app-text)").text(name),

                        // Quantity stepper
                        div("").style("display:flex;align-items:center;gap:4px").children(
                                spActionButton("s")
                                        .children(span("bi bi-dash").style("font-size:0.7rem"))
                                        .on("click", evt -> safeAction("Decrement",
                                                () -> this.presenter.onModifyQuantity(item.id, item.quantity - 1))),
                                span("")
                                        .style("font-weight:700;min-width:24px;text-align:center;font-size:0.85rem;color:var(--app-text)")
                                        .text(String.valueOf(item.quantity)),
                                spActionButton("s")
                                        .children(span("bi bi-plus").style("font-size:0.7rem"))
                                        .on("click", evt -> safeAction("Increment",
                                                () -> this.presenter.onModifyQuantity(item.id, item.quantity + 1)))),

                        // Subtotal
                        span("")
                                .style("width:90px;text-align:right;font-weight:700;color:var(--app-accent);font-size:0.85rem")
                                .text(subtotal),

                        // Remove button
                        spActionButton("s")
                                .children(span("bi bi-x-lg").style("font-size:0.7rem;color:#dc3545"))
                                .on("click", evt -> safeAction("Remove item",
                                        () -> this.presenter.onRemoveProduct(item.id))));
        // @formatter:on
    }

    private double computeTotalCost() {
        if (this.state.items == null)
            return 0;
        double total = 0;
        for (var item : this.state.items) {
            total += item.price * item.quantity;
        }
        return total;
    }
}
