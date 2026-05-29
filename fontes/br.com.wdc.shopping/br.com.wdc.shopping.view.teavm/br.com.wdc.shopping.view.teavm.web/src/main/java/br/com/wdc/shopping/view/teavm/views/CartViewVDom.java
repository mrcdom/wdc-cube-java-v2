package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
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

        return div("")
                .style(PAGE_WRAPPER)
                .children(
                        div("")
                                .style(CARD)
                                .children(
                                        // Title row
                                        div("d-flex align-items-center gap-2 mb-3").children(
                                                span(BsIcons.CART + " text-primary"),
                                                h5("mb-0 fw-bold").text("Carrinho")),

                                        h6("")
                                                .style(SECTION_LABEL + ";margin:0 0 12px 0")
                                                .text("LISTA DE PRODUTOS"),

                                        // Error
                                        div("alert alert-danger mb-3")
                                                .style(showError ? "" : "display:none")
                                                .text(errorMessage),

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
                                .style(EMPTY_STATE_ICON)
                                .children(span(BsIcons.CART)),
                        p("").style("color:" + TEXT_SECONDARY + ";font-size:1.1rem").text("Seu carrinho está vazio"),
                        span("")
                                .style("cursor:pointer;color:" + PRIMARY + ";font-weight:bold;font-size:1rem")
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
                                .style(SEPARATOR)
                                .children(
                                        span("fw-bold").text("VALOR TOTAL: "),
                                        span("")
                                                .style("font-size:18px;" + PRICE + ";margin-left:8px")
                                                .text(totalText)),

                        // Actions
                        div("d-flex justify-content-between align-items-center mt-3").children(
                                button("btn btn-link p-0")
                                        .style(BTN_LINK)
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
                .style(LIST_ITEM)
                .children(
                        span("").style("flex:1;font-weight:500;font-size:0.9rem").text(name),
                        span("")
                                .style("width:100px;text-align:right;" + PRICE + ";font-size:0.9rem")
                                .text(subtotal),
                        span("")
                                .style("width:50px;text-align:center;font-size:0.85rem;color:" + TEXT_SECONDARY)
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
