package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.Map;

import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.vdom.VNode;

/**
 * Product detail view.
 * State: product {id, name, image, price, description}, errorMessage, errorCode.
 */
public class ProductView extends AbstractRemoteView {

    public static final String VIEW_ID = "48b693f67410";

    private static final int ON_BACK = 1;
    private static final int ON_ADD_TO_CART = 2;

    @SuppressWarnings("java:S1214")
    private interface Styles {

        String ROOT = css()
                .flex("1")
                .minHeight("0")
                .overflowY("auto")
                .background("var(--app-bg)")
                .build();

        String WRAPPER = css(
                ).maxWidth("900px")
                .margin("0 auto")
                .padding("20px")
                .build();

        String TITLE = css()
                .fontWeight("700")
                .margin("0 0 12px 0")
                .fontSize("1.5rem")
                .color("var(--app-text)")
                .build();

        String DIVIDER = css()
                .margin("0 0 16px 0")
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
                .display("inline-block")
                .background("var(--app-accent-light)")
                .padding("10px 20px")
                .borderRadius("var(--app-radius-sm)")
                .fontSize("1.4rem").
                fontWeight("800")
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
                .height("auto")
                .objectFit("contain")
                .build();

        String ACTIONS_ROW = css()
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .gap("12px")
                .marginTop("8px")
                .build();

        String ICON_MR = css()
                .marginRight("6px")
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

    private int quantity = 1;
    private String currentDescription = "";

    public ProductView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        Map<String, Object> product = scope.getMap("product");
        String imageUrl = "";
        String name = "";
        String price = "";
        String description = "";

        if (!product.isEmpty()) {
            var img = product.get("image");
            imageUrl = img != null ? "/" + img : "";
            var n = product.get("name");
            name = n != null ? n.toString() : "";
            var p = product.get("price");
            price = p instanceof Number num ? "R$ " + String.format("%.2f", num.doubleValue()) : "";
            var d = product.get("description");
            description = d != null ? d.toString() : "";
        }

        final String desc = description;

        // @formatter:off
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            h5().style(Styles.TITLE).text(name),
            spDivider("s").style(Styles.DIVIDER),
            // Description card
            div().style(Styles.DESC_CARD).children(
              div().style(Styles.DESC_TEXT)
                .ref(el -> {
                    if (!desc.equals(this.currentDescription)) {
                        el.setInnerHTML(desc);
                        this.currentDescription = desc;
                    }
                })),
            // Price + Image row
            div().style(Styles.PRICE_IMAGE_ROW).children(
              div().style(Styles.PRICE_COL).children(
                span().style(Styles.PRICE_BADGE).text(price),
                div().style(Styles.QTY_ROW).children(
                  span().style(Styles.QTY_LABEL).text("Qtd:"),
                  spActionButton("s")
                    .children(span("bi bi-dash"))
                    .on("click", evt -> { if (this.quantity > 1) { this.quantity--; forceUpdate(); } }),
                  span().style(Styles.QTY_VALUE).text(String.valueOf(this.quantity)),
                  spActionButton("s")
                    .children(span("bi bi-plus"))
                    .on("click", evt -> { this.quantity++; forceUpdate(); }))),
              div().style(Styles.IMAGE_BOX).children(
                img().attr("alt", name).attr("src", imageUrl).style(Styles.IMAGE))),
            // Action buttons
            div().style(Styles.ACTIONS_ROW).children(
              spActionButton()
                .children(span("bi bi-arrow-left"), span().text(" Voltar"))
                .on("click", evt -> submit(ON_BACK)),
              spButton("accent", "l")
                .children(span("bi bi-bag-plus").style(Styles.ICON_MR), span().text("Adicionar ao Carrinho"))
                .on("click", evt -> { setFormField("p.quantity", this.quantity); submit(ON_ADD_TO_CART); }))),
          // Error
          div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
            span("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
            span().style(Styles.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")));
        // @formatter:on
    }
}
