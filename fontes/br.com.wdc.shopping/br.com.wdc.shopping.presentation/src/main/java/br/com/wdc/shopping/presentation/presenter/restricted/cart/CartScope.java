package br.com.wdc.shopping.presentation.presenter.restricted.cart;

import java.util.Collections;
import java.util.List;

import br.com.wdc.framework.cube.Action;
import br.com.wdc.framework.cube.ActionOf;
import br.com.wdc.framework.cube.Prop;
import br.com.wdc.framework.cube.Scope;

/** What the cart draws ({@link CartPresenter}): one row per item, each a segment of its own, and the total. */
public class CartScope extends Scope {

    public static final String CLASS_ID = "7eb485e5f843";

    /**
     * A row of the cart: a segment with its own view and its own actions, bound to its item — the row does not
     * send the product id back, it is the item.
     */
    public static class CartItemScope extends Scope {

        public static final String CLASS_ID = "c8a1f0e2b7d4";

        public final Prop<Long> id = prop();
        public final Prop<String> name = prop();
        public final Prop<String> image = prop();
        public final Prop<Double> price = prop(0.0);
        public final Prop<Integer> quantity = prop(0);

        public Action onRemove = Action.NOOP;
        public ActionOf<Integer> onModifyQuantity = ActionOf.noop();

    }

    /** The rows, in the cart's order; changing a quantity redraws only its row (and the total). */
    public final Prop<List<CartItemScope>> rows = prop(Collections.emptyList());
    public final Prop<Double> total = prop(0.0);
    public final Prop<Integer> errorCode = prop(0);
    public final Prop<String> errorMessage = prop();

    public Action onBuy = Action.NOOP;
    public Action onOpenProducts = Action.NOOP;

}
