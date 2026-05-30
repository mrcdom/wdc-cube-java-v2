package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class ProductViewVDom extends AbstractVDomView<ProductPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String ROOT = css()
                .flex("1")
                .minHeight("0")
                .overflowY("auto")
                .background("var(--app-bg)")
                .build();

        String WRAPPER = css()
                .maxWidth("900px")
                .prop("margin", "0 auto")
                .padding("20px")
                .build();

        String TITLE = css()
                .fontWeight("700")
                .prop("margin", "0 0 12px 0")
                .fontSize("1.5rem")
                .color("var(--app-text)")
                .build();

        String DIVIDER = css()
                .prop("margin", "0 0 16px 0")
                .build();

        String DESC_CARD = css()
                .background("var(--app-surface)")
                .borderRadius("var(--app-radius)")
                .border("1px solid var(--app-border)")
                .padding("20px")
                .marginBottom("20px")
                .build();

        String DESC_TEXT = css()
                .fontSize("0.9rem")
                .lineHeight("1.7")
                .color("var(--app-text)")
                .build();

        String PRICE_IMAGE_ROW = css()
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .gap("32px")
                .marginBottom("16px")
                .flexWrap("wrap")
                .build();

        String PRICE_COL = css()
                .flexCol()
                .alignItems("center")
                .gap("12px")
                .build();

        String PRICE_BADGE = css()
                .prop("display", "inline-block")
                .background("var(--app-accent-light)")
                .padding("10px 20px")
                .borderRadius("var(--app-radius-sm)")
                .fontSize("1.4rem")
                .fontWeight("800")
                .color("var(--app-accent)")
                .build();

        String QTY_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .build();

        String QTY_LABEL = css()
                .fontSize("0.85rem")
                .color("var(--app-text-secondary)")
                .build();

        String QTY_VALUE = css()
                .fontWeight("700")
                .minWidth("28px")
                .textAlign("center")
                .fontSize("1.1rem")
                .build();

        String IMAGE_BOX = css()
                .background("linear-gradient(180deg, #f8fafc 0%, #eef2f7 100%)")
                .borderRadius("var(--app-radius-sm)")
                .padding("16px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .build();

        String IMAGE = css()
                .maxWidth("160px")
                .maxHeight("160px")
                .width("100%")
                .prop("height", "auto")
                .objectFit("contain")
                .build();

        String ACTIONS_ROW = css()
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .gap("12px")
                .marginTop("8px")
                .build();

        String ERROR_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("12px 16px")
                .background("#fef2f2")
                .border("1px solid #fecaca")
                .borderRadius("var(--app-radius-sm)")
                .marginTop("12px")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String ERROR_ICON = css()
                .color("#dc2626")
                .fontSize("1rem")
                .flexShrink(0)
                .build();

        String ERROR_TEXT = css()
                .fontSize("0.85rem")
                .color("#991b1b")
                .fontWeight("500")
                .build();
    }

    private final ProductViewState state;
    private int quantity = 1;
    private String currentDescription = "";

    public ProductViewVDom(ProductPresenter presenter) {
        super("product", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode != 0) {
            showError = true;
            errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            showError = false;
            errorMessage = "";
        }

        var product = this.state.product;
        var imageUrl = "";
        var name = "";
        var price = "";
        final String description;

        if (product != null) {
            imageUrl = product.image != null ? app.resolveImageUrl(product.image) : "";
            name = product.name != null ? product.name : "";
            price = product.price > 0 ? "R$ " + String.format("%.2f", product.price) : "";
            description = product.description != null ? product.description : "";
        } else {
            description = "";
        }

        // @formatter:off
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            // Title
            h5().style(Styles.TITLE).text(name),
            // Divider
            spDivider("s").style(Styles.DIVIDER),
            // Description card
            div().style(Styles.DESC_CARD).children(
              div().style(Styles.DESC_TEXT)
                .ref(el -> {
                    if (!description.equals(this.currentDescription)) {
                        el.setInnerHTML(description);
                        this.currentDescription = description;
                    }
                })),
            // Price + Image row
            div().style(Styles.PRICE_IMAGE_ROW).children(
              // Left: price + quantity (centered)
              div().style(Styles.PRICE_COL).children(
                span().style(Styles.PRICE_BADGE).text(price),
                // Quantity stepper
                div().style(Styles.QTY_ROW).children(
                  span().style(Styles.QTY_LABEL).text("Qtd:"),
                  spActionButton("s")
                    .children(span("bi bi-dash"))
                    .on("click", evt -> { if (this.quantity > 1) { this.quantity--; update(); } }),
                  span().style(Styles.QTY_VALUE).text(String.valueOf(this.quantity)),
                  spActionButton("s")
                    .children(span("bi bi-plus"))
                    .on("click", evt -> { this.quantity++; update(); }))),
              // Right: product image
              div().style(Styles.IMAGE_BOX).children(
                img().attr("alt", name).attr("src", imageUrl).style(Styles.IMAGE))),
            // Action buttons row
            div().style(Styles.ACTIONS_ROW).children(
              spActionButton()
                .children(span("bi bi-arrow-left"), span().text(" Voltar"))
                .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
              spButton("accent", "l")
                .children(span("bi bi-bag-plus").style("margin-right:6px"), span().text("Adicionar ao Carrinho"))
                .on("click", evt -> safeAction("AddToCart", () -> this.presenter.onAddToCart(this.quantity))))),
          // Error
          div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
            span("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
            span().style(Styles.ERROR_TEXT).text(errorMessage)));
        // @formatter:on
    }
}
