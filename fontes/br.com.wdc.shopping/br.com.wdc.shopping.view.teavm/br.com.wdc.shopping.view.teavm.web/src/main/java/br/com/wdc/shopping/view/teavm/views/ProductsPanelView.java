package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.VNode.*;

import java.util.List;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class ProductsPanelView extends AbstractVDomView<ProductsPanelPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Sel {

        String PANEL = "products-panel";
        String CARD_IMAGE_WRAP = "products-card-image-wrap";
        String CARD_IMAGE = "products-card-image";
        String CARD_BODY = "products-card-body";
        String CARD_NAME = "products-card-name";
        String CARD_PRICE = "products-card-price";
    }

    private final ProductsPanelViewState state;

    private EventListener<Event> mkOnOpenProduct(long id) {
        return evt -> safeAction("Open product", () -> this.presenter.onOpenProduct(id));
    }

    public ProductsPanelView(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        // @formatter:off
        return div(Sel.PANEL).children(
          div("product-grid")
            .children(products != null ? products.stream().map(this::renderCard).toList() : List.of()));
        // @formatter:on
    }

    private VNode renderCard(ProductInfo product) {
        var imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
        var name = product.name != null ? product.name : "";
        var price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";
        var key = String.valueOf(product.id);

        // @formatter:off
        return div("product-card").key(key)
          .on("click", useCallback("open-" + key, mkOnOpenProduct(product.id)))
          .children(
            div(Sel.CARD_IMAGE_WRAP).children(
              img().attr("alt", name).attr("src", imageUrl).cls(Sel.CARD_IMAGE)),
            div(Sel.CARD_BODY).children(
              p(Sel.CARD_NAME).text(name),
              span(Sel.CARD_PRICE).text(price)));
        // @formatter:on
    }
}
