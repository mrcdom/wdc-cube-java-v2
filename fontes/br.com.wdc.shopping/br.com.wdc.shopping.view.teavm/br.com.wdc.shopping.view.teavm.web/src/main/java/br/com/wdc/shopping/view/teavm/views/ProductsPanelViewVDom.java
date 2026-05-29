package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
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
        this.element.setAttribute("style", "flex:1;min-width:0;min-height:0;overflow-y:auto");
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        return div("").children(
                h6("fw-bold text-uppercase mb-2")
                        .style(SECTION_LABEL)
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
                        .style(PRODUCT_CARD)
                        .on("click", evt -> safeAction("Open product", () -> this.presenter.onOpenProduct(product.id)))
                        .children(
                                // Image pane
                                div("d-flex align-items-center justify-content-center")
                                        .style(PRODUCT_IMAGE_PANE)
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
                                                        .style("font-size:0.85rem;font-weight:500;margin:0 0 8px 0;color:" + TEXT_DARK)
                                                        .text(name),
                                                span("")
                                                        .style(PRICE_MD)
                                                        .text(price))));
    }
}
