package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class ProductsPanelViewVDom extends AbstractVDomView<ProductsPanelPresenter> {

    private final ProductsPanelViewState state;

    public ProductsPanelViewVDom(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        // @formatter:off
        return div("").style("flex:1;min-width:0;min-height:0;overflow-y:auto;padding:20px").children(
                div("product-grid")
                        .children(products != null ? products.stream().map(this::renderCard).toList() : List.of()));
        // @formatter:on
    }

    private VNode renderCard(ProductInfo product) {
        var imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
        var name = product.name != null ? product.name : "";
        var price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";

        // @formatter:off
        return div("product-card").key(String.valueOf(product.id))
                .on("click", evt -> safeAction("Open product", () -> this.presenter.onOpenProduct(product.id)))
                .children(
                        // Image pane with gradient background
                        div("")
                                .style("background:linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%);padding:20px;display:flex;align-items:center;justify-content:center;aspect-ratio:1")
                                .children(
                                        img("")
                                                .attr("alt", name)
                                                .attr("src", imageUrl)
                                                .style("width:80%;height:80%;object-fit:contain;transition:transform 0.3s cubic-bezier(0.4,0,0.2,1)")),
                        // Label group
                        div("")
                                .style("padding:14px 16px")
                                .children(
                                        p("")
                                                .style("font-size:0.82rem;font-weight:500;margin:0 0 6px 0;color:var(--app-text);line-height:1.3;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden")
                                                .text(name),
                                        span("")
                                                .style("font-size:1.05rem;font-weight:700;color:var(--app-accent)")
                                                .text(price)));
        // @formatter:on
    }
}
