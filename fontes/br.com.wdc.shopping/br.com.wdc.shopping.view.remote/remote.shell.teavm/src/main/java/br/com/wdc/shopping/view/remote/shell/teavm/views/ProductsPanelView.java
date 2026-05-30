package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.vdom.VNode;

/**
 * Products grid panel.
 * State: products (array of {id, name, image, price}).
 */
public class ProductsPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "a1b2c3d4e5f6";

    private static final int ON_OPEN_PRODUCT = 1;

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String PANEL = css()
                .flex("1")
                .minWidth("0")
                .minHeight("0")
                .overflowY("auto")
                .padding("20px")
                .build();

        String CARD_IMAGE_WRAP = css()
                .background("linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .padding("20px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .aspectRatio("1")
                .build();

        String CARD_IMAGE = css()
                .width("80%")
                .height("80%")
                .objectFit("contain")
                .transition("transform 0.3s cubic-bezier(0.4,0,0.2,1)")
                .build();

        String CARD_BODY = css()
                .padding("14px 16px")
                .build();

        String CARD_NAME = css()
                .fontSize("0.82rem")
                .fontWeight("500")
                .margin("0 0 6px 0")
                .color("var(--app-text)")
                .lineHeight("1.3")
                .display("-webkit-box")
                .prop("-webkit-line-clamp", "2")
                .prop("-webkit-box-orient", "vertical")
                .overflowHidden()
                .build();

        String CARD_PRICE = css()
                .fontSize("1.05rem")
                .fontWeight("700")
                .color("var(--app-accent)")
                .build();
    }

    public ProductsPanelView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        List<Map<String, Object>> products = getProducts(scope);

        // @formatter:off
        return div().style(Styles.PANEL).children(
          div("product-grid")
            .children(products.stream().map(this::renderCard).toList()));
        // @formatter:on
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> getProducts(br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope scope) {
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
            div().style(Styles.CARD_IMAGE_WRAP).children(
              img().attr("alt", name).attr("src", image).style(Styles.CARD_IMAGE)),
            div().style(Styles.CARD_BODY).children(
              p().style(Styles.CARD_NAME).text(name),
              span().style(Styles.CARD_PRICE).text(price)));
        // @formatter:on
    }
}
