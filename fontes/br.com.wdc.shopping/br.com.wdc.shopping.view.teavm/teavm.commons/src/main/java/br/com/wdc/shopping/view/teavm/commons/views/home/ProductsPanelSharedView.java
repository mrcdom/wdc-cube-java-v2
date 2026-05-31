package br.com.wdc.shopping.view.teavm.commons.views.home;

import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.img;
import static br.com.wdc.shopping.view.teavm.commons.VNode.p;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared Products grid panel view.
 */
public class ProductsPanelSharedView extends SharedVDomView {

    @SuppressWarnings("java:S1214")
    private interface Sel {
        String PANEL = "products-panel";
        String CARD_IMAGE_WRAP = "products-card-image-wrap";
        String CARD_IMAGE = "products-card-image";
        String CARD_BODY = "products-card-body";
        String CARD_NAME = "products-card-name";
        String CARD_PRICE = "products-card-price";
    }

    // -- External bindings --

    public Supplier<ProductsPanelViewState> stateSupplier;
    public Consumer<Long> onOpenProduct;
    public UnaryOperator<String> imageResolver = path -> "/" + path;

    // -- Render --

    private EventListener<Event> mkOnOpenProduct(long id) {
        return evt -> { if (onOpenProduct != null) onOpenProduct.accept(id); };
    }

    @Override
    public VNode render() {
        var state = stateSupplier.get();
        var products = state.products;

        // @formatter:off
        return div(Sel.PANEL).children(
          div("product-grid")
            .children(products != null ? products.stream().map(this::renderCard).toList() : List.of()));
        // @formatter:on
    }

    private VNode renderCard(ProductInfo product) {
        var imageUrl = product.image != null ? imageResolver.apply(product.image) : "";
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
