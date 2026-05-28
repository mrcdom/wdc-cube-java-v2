package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class CartViewVDom extends AbstractVDomView<CartPresenter> {

    private final CartViewState state;

    // Estado local para erro (one-shot)
    private boolean showError;
    private String errorMessage = "";

    public CartViewVDom(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        if (this.state.errorCode != 0) {
            this.showError = true;
            this.errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            this.showError = false;
            this.errorMessage = "";
        }

        var items = this.state.items;
        var empty = items == null || items.isEmpty();
        var totalCost = computeTotalCost();
        var totalText = "R$ " + String.format("%.2f", totalCost);

        return div("")
                .style("max-width:900px;margin:0 auto;padding:12px")
                .children(
                        div("")
                                .style("background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;padding:16px")
                                .children(
                                        // Title row
                                        div("d-flex align-items-center gap-2 mb-3").children(
                                                span(BsIcons.CART + " text-primary"),
                                                h5("mb-0 fw-bold").text("Carrinho")),

                                        h6("")
                                                .style("color:#666;font-size:0.85rem;margin:0 0 12px 0")
                                                .text("LISTA DE PRODUTOS"),

                                        // Error
                                        div("alert alert-danger mb-3")
                                                .style(this.showError ? "" : "display:none")
                                                .text(this.errorMessage),

                                        // Empty cart state
                                        renderEmptyState(empty),

                                        // Cart content
                                        renderContent(items, empty, totalText)));
    }

    private VNode renderEmptyState(boolean empty) {
        return div(empty
                        ? "d-flex flex-column align-items-center justify-content-center py-5"
                        : "d-none")
                .children(
                        div("")
                                .style("width:120px;height:120px;background-color:#e3f2fd;border-radius:50%;"
                                        + "display:flex;align-items:center;justify-content:center;margin-bottom:16px;"
                                        + "font-size:48px;color:#1976d2")
                                .children(span("bi bi-cart3")),
                        p("").style("color:#666;font-size:1.1rem").text("Seu carrinho está vazio"),
                        span("")
                                .style("cursor:pointer;color:#1976d2;font-weight:bold;font-size:1rem")
                                .text("Vamos às compras!")
                                .on("click", evt -> safeAction("Go shopping", this.presenter::onOpenProducts)));
    }

    private VNode renderContent(List<CartItem> items, boolean empty, String totalText) {
        return div("")
                .style(empty ? "display:none" : "")
                .children(
                        // Items list
                        div("").children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),

                        // Footer
                        div("d-flex justify-content-end align-items-center pt-3 mt-3")
                                .style("border-top:1px solid #e0e0e0")
                                .children(
                                        span("fw-bold").text("VALOR TOTAL: "),
                                        span("")
                                                .style("font-size:18px;font-weight:bold;color:#1976d2;margin-left:8px")
                                                .text(totalText)),

                        // Actions
                        div("d-flex justify-content-between align-items-center mt-3").children(
                                button("btn btn-link p-0")
                                        .style("color:#1976d2;text-decoration:underline;font-size:0.85rem")
                                        .children(
                                                span(BsIcons.ARROW_BACK),
                                                span("").text(" Voltar aos produtos"))
                                        .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
                                button("btn btn-success").children(
                                        span(BsIcons.BAG),
                                        span("").text(" FINALIZAR PEDIDO"))
                                        .on("click", evt -> safeAction("Buy", this.presenter::onBuy))));
    }

    private VNode renderItem(CartItem item) {
        var name = item.name != null ? item.name : "";
        var subtotal = "R$ " + String.format("%.2f", item.price * item.quantity);
        var qty = "x" + item.quantity;

        return div("")
                .key(String.valueOf(item.id))
                .style("display:flex;align-items:center;padding:10px 0;border-bottom:1px solid #f0f0f0")
                .children(
                        span("").style("flex:1;font-weight:500;font-size:0.9rem").text(name),
                        span("")
                                .style("width:100px;text-align:right;font-weight:bold;color:#1976d2;font-size:0.9rem")
                                .text(subtotal),
                        span("")
                                .style("width:50px;text-align:center;font-size:0.85rem;color:#666")
                                .text(qty),
                        button("btn btn-sm btn-outline-danger ms-2")
                                .children(span(BsIcons.TRASH))
                                .on("click", evt -> safeAction("Remove item",
                                        () -> this.presenter.onRemoveProduct(item.id))));
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
