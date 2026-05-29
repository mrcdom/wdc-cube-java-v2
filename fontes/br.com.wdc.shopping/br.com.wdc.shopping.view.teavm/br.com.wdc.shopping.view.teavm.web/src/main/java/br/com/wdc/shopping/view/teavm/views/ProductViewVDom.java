package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.Swc.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
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
        return div("").style("flex:1;min-height:0;overflow-y:auto;background:var(--app-bg)").children(
                div("")
                        .style("max-width:900px;margin:0 auto;padding:20px")
                        .children(
                        // Title
                        h5("").style("font-weight:700;margin:0 0 12px 0;font-size:1.5rem;color:var(--app-text)").text(name),

                        // Divider
                        spDivider("s").style("margin:0 0 16px 0"),

                        // Description card
                        div("")
                                .style("background:var(--app-surface);border-radius:var(--app-radius);border:1px solid var(--app-border);padding:20px;margin-bottom:20px")
                                .children(
                                        div("")
                                                .style("font-size:0.9rem;line-height:1.7;color:var(--app-text)")
                                                .ref(el -> {
                                                    if (!description.equals(this.currentDescription)) {
                                                        el.setInnerHTML(description);
                                                        this.currentDescription = description;
                                                    }
                                                })),

                        // Price + Image row
                        div("")
                                .style("display:flex;align-items:center;justify-content:center;gap:32px;margin-bottom:16px;flex-wrap:wrap")
                                .children(
                                        // Left: price + quantity (centered)
                                        div("").style("display:flex;flex-direction:column;align-items:center;gap:12px").children(
                                                // Price badge
                                                span("")
                                                        .style("display:inline-block;background:var(--app-accent-light);padding:10px 20px;border-radius:var(--app-radius-sm);font-size:1.4rem;font-weight:800;color:var(--app-accent)")
                                                        .text(price),

                                                // Quantity stepper
                                                div("").style("display:flex;align-items:center;gap:10px").children(
                                                        span("").style("font-size:0.85rem;color:var(--app-text-secondary)").text("Qtd:"),
                                                        spActionButton("s")
                                                                .children(span("bi bi-dash"))
                                                                .on("click", evt -> {
                                                                    if (this.quantity > 1) {
                                                                        this.quantity--;
                                                                        update();
                                                                    }
                                                                }),
                                                        span("")
                                                                .style("font-weight:700;min-width:28px;text-align:center;font-size:1.1rem")
                                                                .text(String.valueOf(this.quantity)),
                                                        spActionButton("s")
                                                                .children(span("bi bi-plus"))
                                                                .on("click", evt -> {
                                                                    this.quantity++;
                                                                    update();
                                                                }))),

                                        // Right: product image
                                        div("")
                                                .style("background:linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);border-radius:var(--app-radius-sm);padding:16px;display:flex;align-items:center;justify-content:center")
                                                .children(
                                                        img("")
                                                                .attr("alt", name)
                                                                .attr("src", imageUrl)
                                                                .style("max-width:160px;max-height:160px;width:100%;height:auto;object-fit:contain"))),

                        // Action buttons row
                        div("")
                                .style("display:flex;align-items:center;justify-content:center;gap:12px;margin-top:8px")
                                .children(
                                        spActionButton()
                                                .children(
                                                        span("bi bi-arrow-left"),
                                                        span("").text(" Voltar"))
                                                .on("click",
                                                        evt -> safeAction("Back", this.presenter::onOpenProducts)),
                                        spButton("accent", "l")
                                                .children(
                                                        span("bi bi-bag-plus").style("margin-right:6px"),
                                                        span("").text("Adicionar ao Carrinho"))
                                                .on("click", evt -> safeAction("AddToCart",
                                                        () -> this.presenter.onAddToCart(this.quantity))))),

                // Error
                div("")
                        .style(showError ? "display:flex;align-items:center;gap:10px;padding:12px 16px;background:#fef2f2;border:1px solid #fecaca;border-radius:var(--app-radius-sm);margin-top:12px" : "display:none")
                        .children(
                                span("bi bi-exclamation-circle").style("color:#dc2626;font-size:1rem;flex-shrink:0"),
                                span("").style("font-size:0.85rem;color:#991b1b;font-weight:500").text(errorMessage)));
        // @formatter:on
    }
}
