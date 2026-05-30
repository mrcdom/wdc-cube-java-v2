package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spButton;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.h5;
import static br.com.wdc.framework.vdom.VNode.p;
import static br.com.wdc.framework.vdom.VNode.span;

import java.util.List;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;

public class CartViewVDom extends AbstractVDomView<CartPresenter> {

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = CssUtility.PAGE_SCROLL_ROOT;
        String WRAPPER = CssUtility.PAGE_WRAPPER;
        String CARD = CssComponents.CARD_PANEL;
        String HEADER_ROW = CssComponents.CARD_HEADER_ROW;
        String HEADER_ICON_BOX = CssComponents.CARD_HEADER_ICON_BOX;
        String HEADER_ICON = clsx(CssIcons.BAG, CssComponents.CARD_HEADER_ICON);
        String HEADER_TITLE = CssComponents.CARD_HEADER_TITLE;
        String HEADER_SUBTITLE = CssComponents.CARD_HEADER_SUBTITLE;
        String ERROR_VISIBLE = clsx(CssComponents.ALERT_ERROR, CssUtility.MB_12);
        String HIDDEN = CssUtility.HIDDEN;
        String ERROR_ICON = clsx(CssIcons.EXCLAMATION_CIRCLE, CssComponents.ALERT_ERROR_ICON);
        String ERROR_TEXT = CssComponents.ALERT_ERROR_TEXT;
        String EMPTY_STATE = clsx(CssComponents.EMPTY_STATE, CssUtility.PY_48);
        String EMPTY_ICON_BOX = "cart-empty-icon-box";
        String EMPTY_ICON = clsx(CssIcons.BAG, "cart-empty-icon");
        String EMPTY_TITLE = "cart-empty-title";
        String EMPTY_SUBTITLE = "cart-empty-subtitle";
        String FOOTER = "cart-footer";
        String FOOTER_LABEL = "cart-footer-label";
        String FOOTER_TOTAL = "cart-footer-total";
        String ACTIONS_ROW = "cart-actions-row";
        String ITEM_ROW = "cart-item-row";
        String ITEM_NAME = "cart-item-name";
        String STEPPER_ROW = "cart-stepper-row";
        String STEPPER_MINUS_ICON = clsx(CssIcons.DASH, "cart-stepper-icon");
        String STEPPER_PLUS_ICON = clsx(CssIcons.PLUS, "cart-stepper-icon");
        String STEPPER_VALUE = "cart-stepper-value";
        String ITEM_SUBTOTAL = "cart-item-subtotal";
        String REMOVE_ICON = clsx(CssIcons.X_LG, "cart-remove-icon");
        String ICON_VIEW_PRODUCTS = clsx(CssIcons.GRID_3X3_GAP, CssUtility.MR_6);
        String ICON_FINALIZE = clsx(CssIcons.CHECK2_CIRCLE, CssUtility.MR_6);
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
        return div(Css.ROOT).children(
          div(Css.WRAPPER).children(
            div(Css.CARD).children(
              // Title row
              div(Css.HEADER_ROW).children(
                div(Css.HEADER_ICON_BOX)
                  .children(span(Css.HEADER_ICON)),
                div().children(
                  h5(Css.HEADER_TITLE).text("Carrinho"),
                  span(Css.HEADER_SUBTITLE).text("Seus produtos selecionados"))),
              // Error
              div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
                span(Css.ERROR_ICON),
                span(Css.ERROR_TEXT).text(errorMessage)),
              // Empty cart state
              renderEmptyState(empty),
              // Cart content
              renderContent(items, empty, totalText))));
        // @formatter:on
    }

    private VNode renderEmptyState(boolean empty) {
        // @formatter:off
        return div(empty ? Css.EMPTY_STATE : Css.HIDDEN).children(
          div(Css.EMPTY_ICON_BOX)
            .children(span(Css.EMPTY_ICON)),
          p(Css.EMPTY_TITLE).text("Carrinho vazio"),
          p(Css.EMPTY_SUBTITLE).text("Adicione produtos para começar"),
          spButton("accent")
            .children(span(Css.ICON_VIEW_PRODUCTS), span().text("Ver produtos"))
            .on("click", evt -> safeAction("Go shopping", this.presenter::onOpenProducts)));
        // @formatter:on
    }

    private VNode renderContent(List<CartItem> items, boolean empty, String totalText) {
        // @formatter:off
        return div(empty ? Css.HIDDEN : "").children(
          // Items list
          div().children(items != null ? items.stream().map(this::renderItem).toList() : List.of()),
          // Footer
          div(Css.FOOTER).children(
            span(Css.FOOTER_LABEL).text("Total: "),
            span(Css.FOOTER_TOTAL).text(totalText)),
          // Actions
          div(Css.ACTIONS_ROW).children(
            spActionButton()
              .children(span(CssIcons.ARROW_LEFT), span().text(" Continuar comprando"))
              .on("click", evt -> safeAction("Back", this.presenter::onOpenProducts)),
            spButton("accent", "l")
              .children(span(Css.ICON_FINALIZE), span().text("Finalizar pedido"))
              .on("click", evt -> safeAction("Buy", this.presenter::onBuy))));
        // @formatter:on
    }

    private VNode renderItem(CartItem item) {
        var name = item.name != null ? item.name : "";
        var subtotal = "R$ " + String.format("%.2f", item.price * item.quantity);

        // @formatter:off
        return div(Css.ITEM_ROW).key(String.valueOf(item.id)).children(
          span(Css.ITEM_NAME).text(name),
          // Quantity stepper
          div(Css.STEPPER_ROW).children(
            spActionButton("s")
              .children(span(Css.STEPPER_MINUS_ICON))
              .on("click", evt -> safeAction("Decrement",
                  () -> this.presenter.onModifyQuantity(item.id, item.quantity - 1))),
            span(Css.STEPPER_VALUE).text(String.valueOf(item.quantity)),
            spActionButton("s")
              .children(span(Css.STEPPER_PLUS_ICON))
              .on("click", evt -> safeAction("Increment",
                  () -> this.presenter.onModifyQuantity(item.id, item.quantity + 1)))),
          // Subtotal
          span(Css.ITEM_SUBTOTAL).text(subtotal),
          // Remove button
          spActionButton("s")
            .children(span(Css.REMOVE_ICON))
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
