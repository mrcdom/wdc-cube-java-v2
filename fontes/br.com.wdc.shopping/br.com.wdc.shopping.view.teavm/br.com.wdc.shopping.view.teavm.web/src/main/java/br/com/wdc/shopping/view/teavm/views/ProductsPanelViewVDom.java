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
        this.element.getClassList().add("p-3");
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        return div("").children(
                h6("fw-bold text-uppercase mb-2")
                        .style("color:#666;font-size:0.85rem")
                        .text("PRODUTOS"),
                div("row row-cols-2 row-cols-md-3 g-3")
                        .children(products != null ? products.stream().map(this::renderCard).toList() : List.of()));
    }

    private VNode renderCard(ProductInfo product) {
        var imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
        var name = product.name != null ? product.name : "";
        var price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";

        return div("col").key(String.valueOf(product.id)).children(
                div("")
                        .style("background-color:#fff;border-radius:12px;border:1px solid #e0e0e0;"
                                + "cursor:pointer;transition:all 0.25s cubic-bezier(0.4,0,0.2,1);overflow:hidden")
                        .on("click", evt -> safeAction("Open product", () -> this.presenter.onOpenProduct(product.id)))
                        .children(
                                // Image pane
                                div("d-flex align-items-center justify-content-center")
                                        .style("background-color:#f8f9fa;padding:16px;border-bottom:1px solid #f0f0f0")
                                        .children(
                                                img("")
                                                        .attr("alt", "Produto")
                                                        .attr("src", imageUrl)
                                                        .style("width:100px;height:100px;object-fit:contain")),
                                // Label group
                                div("")
                                        .style("padding:16px")
                                        .children(
                                                p("")
                                                        .style("font-size:0.85rem;font-weight:500;margin:0 0 8px 0;color:#333")
                                                        .text(name),
                                                span("")
                                                        .style("font-size:1.1rem;font-weight:bold;color:#1976d2")
                                                        .text(price))));
    }
}
