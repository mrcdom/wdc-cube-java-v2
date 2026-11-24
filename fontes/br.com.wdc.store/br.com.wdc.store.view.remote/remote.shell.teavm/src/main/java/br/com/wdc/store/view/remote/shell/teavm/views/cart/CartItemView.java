package br.com.wdc.store.view.remote.shell.teavm.views.cart;

import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.cart.CartItemSharedView;

/** A row of the cart: a view of its own, whose actions are the row's (no product id goes back). */
public class CartItemView extends AbstractRemoteView {

    public static final String VIEW_ID = CartScope.CartItemScope.CLASS_ID;

    private static final int ON_REMOVE = 1;
    private static final int ON_MODIFY_QUANTITY = 2;

    private final CartItemSharedView shared;
    private final CartScope.CartItemScope adaptedState = new CartScope.CartItemScope();

    public CartItemView(String vsid) {
        super(vsid);

        this.shared = new CartItemSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.onModifyQuantity = qty -> {
            setFormField("p.quantity", qty);
            submit(ON_MODIFY_QUANTITY);
        };
        this.shared.onRemove = () -> submit(ON_REMOVE);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private CartScope.CartItemScope adaptState() {
        ViewScope scope = state();
        adaptedState.name.set(scope.getString("name"));
        adaptedState.price.set(scope.getDouble("price"));
        adaptedState.quantity.set(scope.getInt("quantity"));
        return adaptedState;
    }
}
