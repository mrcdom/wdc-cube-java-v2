package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.VNode.*;

import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class ProductsPanelViewVDom extends AbstractVDomView<ProductsPanelPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Css {

        String PANEL = "products-panel";
        String CARD_IMAGE_WRAP = "products-card-image-wrap";
        String CARD_IMAGE = "products-card-image";
        String CARD_BODY = "products-card-body";
        String CARD_NAME = "products-card-name";
        String CARD_PRICE = "products-card-price";
    }

    private final ProductsPanelViewState state;

    public ProductsPanelViewVDom(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        // @formatter:off
        return div(Css.PANEL).children(
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
            div(Css.CARD_IMAGE_WRAP).children(
              img().attr("alt", name).attr("src", imageUrl).cls(Css.CARD_IMAGE)),
            div(Css.CARD_BODY).children(
              p(Css.CARD_NAME).text(name),
              span(Css.CARD_PRICE).text(price)));
        // @formatter:on
    }
}
