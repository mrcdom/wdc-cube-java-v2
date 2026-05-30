package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.img;
import static br.com.wdc.framework.vdom.VNode.p;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

/**
 * Products grid panel.
 * State: products (array of {id, name, image, price}).
 */
public class ProductsPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "a1b2c3d4e5f6";

    private static final int ON_OPEN_PRODUCT = 1;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String PANEL = "products-panel";
        String CARD_IMAGE_WRAP = "products-card-image-wrap";
        String CARD_IMAGE = "products-card-image";
        String CARD_BODY = "products-card-body";
        String CARD_NAME = "products-card-name";
        String CARD_PRICE = "products-card-price";
    }

    public ProductsPanelView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        List<Map<String, Object>> products = getProducts(scope);

        // @formatter:off
        return div(Css.PANEL).children(
          div("product-grid")
            .children(products.stream().map(this::renderCard).toList()));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getProducts(ViewScope scope) {
        if (scope == null) return List.of();
        var v = scope.getState().get("products");
        if (v instanceof List<?> list) {
            var result = new ArrayList<Map<String, Object>>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) result.add((Map<String, Object>) m);
            }
            return result;
        }
        return List.of();
    }

    private VNode renderCard(Map<String, Object> product) {
        var id = product.get("id");
        var name = product.get("name") != null ? product.get("name").toString() : "";
        var image = product.get("image") != null ? "/" + product.get("image").toString() : "";
        var priceVal = product.get("price");
        var price = priceVal instanceof Number n ? "R$ " + String.format("%.2f", n.doubleValue()) : "";
        var key = id != null ? id.toString() : name;

        // @formatter:off
        return div("product-card").key(key)
          .on("click", evt -> { setFormField("p.productId", id); submit(ON_OPEN_PRODUCT); })
          .children(
            div(Css.CARD_IMAGE_WRAP).children(
              img().attr("alt", name).attr("src", image).cls(Css.CARD_IMAGE)),
            div(Css.CARD_BODY).children(
              p(Css.CARD_NAME).text(name),
              span(Css.CARD_PRICE).text(price)));
        // @formatter:on
    }
}
