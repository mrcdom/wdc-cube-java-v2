package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.*;
import static br.com.wdc.framework.vdom.VNode.*;

import java.util.List;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.framework.vdom.VNode;

public class CartViewVDom extends AbstractVDomView<CartPresenter> {

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

        String CARD = css()
                .background("var(--app-surface)")
                .borderRadius("var(--app-radius)")
                .border("1px solid var(--app-border)")
                .padding("24px")
                .boxShadow("var(--app-shadow-sm)")
                .build();

        String HEADER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .marginBottom("20px")
                .build();

        String HEADER_ICON_BOX = css()
                .width("40px")
                .height("40px")
                .background("var(--app-accent-light)")
                .borderRadius("10px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .build();

        String HEADER_ICON = css()
                .color("var(--app-accent)")
                .fontSize("1.1rem")
                .build();

        String HEADER_TITLE = css()
                .prop("margin", "0")
                .fontWeight("700")
                .fontSize("1.1rem")
                .color("var(--app-text)")
                .build();

        String HEADER_SUBTITLE = css()
                .fontSize("0.75rem")
                .color("var(--app-text-secondary)")
                .build();

        String ERROR_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("12px 16px")
                .background("#fef2f2")
                .border("1px solid #fecaca")
                .borderRadius("var(--app-radius-sm)")
                .marginBottom("12px")
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

        String EMPTY_STATE = css()
                .flexCol()
                .alignItems("center")
                .justifyContent("center")
                .padding("48px 0")
                .build();

        String EMPTY_ICON_BOX = css()
                .width("100px")
                .height("100px")
                .background("var(--app-accent-light)")
                .borderRadius("50%")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .marginBottom("20px")
                .build();

        String EMPTY_ICON = css()
                .fontSize("2.5rem")
                .color("var(--app-accent)")
                .build();

        String EMPTY_TITLE = css()
                .color("var(--app-text)")
                .fontSize("1.1rem")
                .fontWeight("500")
                .prop("margin", "0 0 8px 0")
                .build();

        String EMPTY_SUBTITLE = css()
                .color("var(--app-text-secondary)")
                .fontSize("0.85rem")
                .prop("margin", "0 0 16px 0")
                .build();

        String FOOTER = css()
                .displayFlex()
                .justifyContent("flex-end")
                .alignItems("center")
                .paddingTop("16px")
                .marginTop("16px")
                .prop("border-top", "1px solid var(--app-border)")
                .build();

        String FOOTER_LABEL = css()
                .fontSize("0.85rem")
                .color("var(--app-text-secondary)")
                .build();

        String FOOTER_TOTAL = css()
                .fontSize("1.4rem")
                .fontWeight("800")
                .color("var(--app-accent)")
                .marginLeft("8px")
                .build();

        String ACTIONS_ROW = css()
                .displayFlex()
                .justifyContent("space-between")
                .alignItems("center")
                .marginTop("16px")
                .build();

        String ITEM_ROW = css()
                .displayFlex()
                .alignItems("center")
                .padding("12px 0")
                .prop("border-bottom", "1px solid var(--app-border)")
                .gap("8px")
                .build();

        String ITEM_NAME = css()
                .flex("1")
                .fontWeight("500")
                .fontSize("0.88rem")
                .color("var(--app-text)")
                .build();

        String STEPPER_ROW = css()
                .displayFlex()
                .alignItems("center")
                .gap("4px")
                .build();

        String STEPPER_ICON = css()
                .fontSize("0.7rem")
                .build();

        String STEPPER_VALUE = css()
                .fontWeight("700")
                .minWidth("24px")
                .textAlign("center")
                .fontSize("0.85rem")
                .color("var(--app-text)")
                .build();

        String ITEM_SUBTOTAL = css()
                .width("90px")
                .textAlign("right")
                .fontWeight("700")
                .color("var(--app-accent)")
                .fontSize("0.85rem")
                .build();

        String REMOVE_ICON = css()
                .fontSize("0.7rem")
                .color("#dc3545")
                .build();
    }

    private final CartViewState state;

    public CartViewVDom(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter);
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

        var items = this.state.items;
        var empty = items == null || items.isEmpty();
        var totalCost = computeTotalCost();
        var totalText = "R$ " + String.format("%.2f", totalCost);

        // @formatter:off
        return div().style(Styles.ROOT).children(
          div().style(Styles.WRAPPER).children(
            div().style(Styles.CARD).children(
              // Title row
              div().style(Styles.HEADER_ROW).children(
                div().style(Styles.HEADER_ICON_BOX)
                  .children(span("bi bi-bag").style(Styles.HEADER_ICON)),
                div().children(
                  h5().style(Styles.HEADER_TITLE).text("Carrinho"),
                  span().style(Styles.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
                span("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
                span().style(Styles.ERROR_TEXT).text(errorMessage)),
              // Empty cart state
              renderEmptyState(empty),
              // Cart content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div().style(empty ? Styles.EMPTY_STATE : Styles.HIDDEN).children(
          div().style(Styles.EMPTY_ICON_BOX)
            .children(span("bi bi-bag").style(Styles.EMPTY_ICON)),
          p().style(Styles.EMPTY_TITLE).text("Carrinho vazio"),
          p().style(Styles.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span("bi bi-grid-3x3-gap").style("margin-right:6px"), span().text("Ver produtos"))
            .on("click", evt -> safeAction("Go shopping", this.presenter::onOpenProducts)));
        // @formatter:on
    }

    private VNode renderContent(List<CartItem> items, boolean empty, String totalText) {
        // @formatter:off
        return div().style(empty ? Styles.HIDDEN : "").children(
          // Items list
          div().children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),
          // Footer
          div().style(Styles.FOOTER).children(
            span().style(Styles.FOOTER_LABEL).text("Total: "),
            span().style(Styles.FOOTER_TOTAL).text(totalText)),
          // Actions
          div().style(Styles.ACTIONS_ROW).children(
            spActionButton()
              .children(span("bi bi-arrow-left"), span().text(" Continuar comprando"))
              .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
            spButton("accent", "l")
              .children(span("bi bi-check2-circle").style("margin-right:6px"), span().text("Finalizar pedido"))
              .on("click", evt -> safeAction("Buy", this.presenter::onBuy))));
        // @formatter:on
    }

    private VNode renderItem(CartItem item) {
        var name = item.name != null ? item.name : "";
        var subtotal = "R$ " + String.format("%.2f", item.price * item.quantity);

        // @formatter:off
        return div().key(String.valueOf(item.id)).style(Styles.ITEM_ROW).children(
          span().style(Styles.ITEM_NAME).text(name),
          // Quantity stepper
          div().style(Styles.STEPPER_ROW).children(
            spActionButton("s")
              .children(span("bi bi-dash").style(Styles.STEPPER_ICON))
              .on("click", evt -> safeAction("Decrement",
                  () -> this.presenter.onModifyQuantity(item.id, item.quantity - 1))),
            span().style(Styles.STEPPER_VALUE).text(String.valueOf(item.quantity)),
            spActionButton("s")
              .children(span("bi bi-plus").style(Styles.STEPPER_ICON))
              .on("click", evt -> safeAction("Increment",
                  () -> this.presenter.onModifyQuantity(item.id, item.quantity + 1)))),
          // Subtotal
          span().style(Styles.ITEM_SUBTOTAL).text(subtotal),
          // Remove button
          spActionButton("s")
            .children(span("bi bi-x-lg").style(Styles.REMOVE_ICON))
            .on("click", evt -> safeAction("Remove item",
                () -> this.presenter.onRemoveProduct(item.id))));
        // @formatter:on
    }

    private double computeTotalCost() {
        if (this.state.items == null)
            return 0;
        double total = 0;
        for (var item : this.state.items) {
            total += item.price * item.quantity;
        }
        return total;
    }
}
