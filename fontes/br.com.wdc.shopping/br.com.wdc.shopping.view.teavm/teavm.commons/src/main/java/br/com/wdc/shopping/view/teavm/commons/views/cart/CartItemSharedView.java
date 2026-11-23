package br.com.wdc.shopping.view.teavm.commons.views.cart;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartScope.CartItemScope;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared view of a cart row: a view of its own, whose actions are the row's (the item is the row — no product id goes
 * back). The cart lays the rows' elements out in its order.
 */
public class CartItemSharedView extends SharedVDomView {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelIcons icon = SelIcons.INSTANCE;

        String ITEM_ROW = "cart-item-row";
        String ITEM_NAME = "cart-item-name";
        String STEPPER_ROW = "cart-stepper-row";
        String STEPPER_MINUS_ICON = clsx(icon.DASH, "cart-stepper-icon");
        String STEPPER_PLUS_ICON = clsx(icon.PLUS, "cart-stepper-icon");
        String STEPPER_VALUE = "cart-stepper-value";
        String ITEM_SUBTOTAL = "cart-item-subtotal";
        String REMOVE_ICON = clsx(icon.X_LG, "cart-remove-icon");
    }

    // -- External bindings --

    public Supplier<CartItemScope> stateSupplier;
    public IntConsumer onModifyQuantity;
    public Runnable onRemove;

    // -- Stable event listeners --

    private final EventListener<Event> minusListener = evt -> onModifyQuantity.accept(quantity() - 1);
    private final EventListener<Event> plusListener = evt -> onModifyQuantity.accept(quantity() + 1);
    private final EventListener<Event> removeListener = evt -> onRemove.run();

    private int quantity() {
        var q = stateSupplier.get().quantity.get();
        return q != null ? q : 0;
    }

    // -- Render --

    @Override
    public VNode render() {
        var row = stateSupplier.get();
        var name = row.name.get() != null ? row.name.get() : "";
        var price = row.price.get() != null ? row.price.get() : 0.0;
        var quantity = quantity();
        var subtotal = "R$ " + String.format("%.2f", price * quantity);

        // @formatter:off
        return div(Sel.ITEM_ROW).children(
          span(Sel.ITEM_NAME).text(name),
          div(Sel.STEPPER_ROW).children(
            spActionButton("s")
              .children(span(Sel.STEPPER_MINUS_ICON))
              .on("click", minusListener),
            span(Sel.STEPPER_VALUE).text(String.valueOf(quantity)),
            spActionButton("s")
              .children(span(Sel.STEPPER_PLUS_ICON))
              .on("click", plusListener)),
          span(Sel.ITEM_SUBTOTAL).text(subtotal),
          spActionButton("s")
            .children(span(Sel.REMOVE_ICON))
            .on("click", removeListener));
        // @formatter:on
    }
}
