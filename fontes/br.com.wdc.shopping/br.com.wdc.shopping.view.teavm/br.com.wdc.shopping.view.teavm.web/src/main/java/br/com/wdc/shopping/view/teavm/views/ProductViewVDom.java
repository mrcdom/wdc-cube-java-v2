package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class ProductViewVDom extends AbstractVDomView<ProductPresenter> {

    private final ProductViewState state;
    private int quantity = 1;
    private String currentDescription = "";

    public ProductViewVDom(ProductPresenter presenter) {
        super("product", (ShoppingTeaVMApplication) presenter.app, presenter);
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

        var product = this.state.product;
        var imageUrl = "";
        var name = "";
        var price = "";
        final String description;

        if (product != null) {
            imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
            name = product.name != null ? product.name : "";
            price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";
            description = product.description != null ? product.description : "";
        } else {
            description = "";
        }

        // @formatter:off
        return div("").style("flex:1;min-height:0;overflow-y:auto").children(
                div("")
                        .style(PAGE_WRAPPER)
                        .children(
                        // Card container
                        div("")
                                .style(CARD)
                                .children(
                                        // Product name
                                        h5("").style("font-weight:bold;margin:0 0 16px 0").text(name),

                                        // Info row
                                        div("d-flex flex-column-reverse flex-sm-row align-items-center gap-3 mb-3")
                                                .children(
                                                        // Left column
                                                        div("d-flex flex-column w-100").children(
                                                                p("")
                                                                        .style(PRICE_LG)
                                                                        .text(price),

                                                                // Quantity stepper
                                                                div("d-flex align-items-center gap-2 mb-3").children(
                                                                        span("").text("Quantidade:"),
                                                                        button("btn btn-sm btn-outline-secondary")
                                                                                .children(span(BsIcons.DASH))
                                                                                .on("click", evt -> {
                                                                                    if (this.quantity > 1) {
                                                                                        this.quantity--;
                                                                                        update();
                                                                                    }
                                                                                }),
                                                                        span("fw-bold")
                                                                                .text(String.valueOf(this.quantity)),
                                                                        button("btn btn-sm btn-outline-secondary")
                                                                                .children(span(BsIcons.PLUS))
                                                                                .on("click", evt -> {
                                                                                    this.quantity++;
                                                                                    update();
                                                                                })),

                                                                // Add to cart button
                                                                button("btn btn-primary").children(
                                                                        span(BsIcons.CART),
                                                                        span("").text(" Adicionar"))
                                                                        .on("click", evt -> safeAction("AddToCart",
                                                                                () -> this.presenter
                                                                                        .onAddToCart(this.quantity)))),

                                                        // Right: product image
                                                        div("text-center").children(
                                                                img("")
                                                                        .attr("alt", "Produto")
                                                                        .attr("src", imageUrl)
                                                                        .style("max-width:200px;max-height:200px;width:100%;height:auto;object-fit:contain"))),

                                        // Description label
                                        span("")
                                                .style(SECTION_LABEL + ";font-weight:600")
                                                .text("Descrição"),

                                        // Description content (HTML from server)
                                        div("")
                                                .style("font-size:0.85rem;line-height:1.5;margin-top:4px")
                                                .ref(el -> {
                                                    if (!description.equals(this.currentDescription)) {
                                                        el.setInnerHTML(description);
                                                        this.currentDescription = description;
                                                    }
                                                }),

                                        // Back button
                                        button("btn btn-link mt-3 p-0")
                                                .style(BTN_LINK)
                                                .children(
                                                        span(BsIcons.ARROW_BACK),
                                                        span("").text(" Voltar aos produtos"))
                                                .on("click",
                                                        evt -> safeAction("Back", this.presenter::onOpenProducts))),

                        // Error
                        div("alert alert-danger m-3")
                                .style(showError ? "" : "display:none")
                                .text(errorMessage)));
        // @formatter:on
    }
}
