package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
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
                .prop("margin", "0 0 6px 0")
                .color("var(--app-text)")
                .lineHeight("1.3")
                .prop("display", "-webkit-box")
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

    private final ProductsPanelViewState state;

    public ProductsPanelViewVDom(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        var products = this.state.products;

        // @formatter:off
        return div().style(Styles.PANEL).children(
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
            div().style(Styles.CARD_IMAGE_WRAP).children(
              img().attr("alt", name).attr("src", imageUrl).style(Styles.CARD_IMAGE)),
            div().style(Styles.CARD_BODY).children(
              p().style(Styles.CARD_NAME).text(name),
              span().style(Styles.CARD_PRICE).text(price)));
        // @formatter:on
    }
}
