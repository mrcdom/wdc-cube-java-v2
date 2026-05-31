package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.img;
import static br.com.wdc.framework.vdom.VNode.p;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

/**
 * Products grid panel. State: products (array of {id, name, image, price}).
 */
public class ProductsPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "a1b2c3d4e5f6";

    private static final int ON_OPEN_PRODUCT = 1;

    @SuppressWarnings("java:S1214")
    private interface Sel {

        String PANEL = "products-panel";
        String CARD_IMAGE_WRAP = "products-card-image-wrap";
        String CARD_IMAGE = "products-card-image";
        String CARD_BODY = "products-card-body";
        String CARD_NAME = "products-card-name";
        String CARD_PRICE = "products-card-price";
    }

    private record Product(Long id, String name, String image, String price) {
    }

    private EventListener<Event> mkOnOpenProduct(Long id) {
        return evt -> {
            setFormField("p.productId", id);
            submit(ON_OPEN_PRODUCT);
        };
    }

    public ProductsPanelView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        var products = getProducts(scope);

        // @formatter:off
        return div(Sel.PANEL).children(
          div("product-grid")
            .children(products.stream().map(this::renderCard).toList()));
        // @formatter:on
    }

    private VNode renderCard(Product product) {
        var key = product.id().toString();
        // @formatter:off
        return div("product-card").key(key)
          .on("click", useCallback("open-" + key, mkOnOpenProduct(product.id())))
          .children(
            div(Sel.CARD_IMAGE_WRAP).children(
              img().attr("alt", product.name()).attr("src", product.image()).cls(Sel.CARD_IMAGE)),
            div(Sel.CARD_BODY).children(
              p(Sel.CARD_NAME).text(product.name()),
              span(Sel.CARD_PRICE).text(product.price())));
        // @formatter:on
    }

    // :: State mapping helpers

    private List<Product> getProducts(ViewScope scope) {
        if (scope == null)
            return List.of();
        var v = scope.getState().get("products");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Product>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m && m.containsKey("id")) {
                    result.add(mapToProduct(m));
                }
            }
            return result;
        }
        return List.of();
    }

    private Product mapToProduct(Map<?, ?> map) {
        var id = CoerceUtils.asLong(map.get("id"));
        var name = CoerceUtils.asString(map.get("name"), "");
        var rawImage = CoerceUtils.asString(map.get("image"));
        var image = rawImage != null ? "/" + rawImage : "";
        var priceVal = CoerceUtils.asNumber(map.get("price"));
        var price = priceVal != null ? "R$ " + String.format("%.2f", priceVal.doubleValue()) : "";
        return new Product(id, name, image, price);
    }

}
