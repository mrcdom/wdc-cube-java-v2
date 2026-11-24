package br.com.wdc.store.view.teavm.views.cart;

import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope.CartItemScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.cart.CartItemSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

/** The view of a cart row: a segment of the cart, with its own state and actions. */
public class CartItemView extends AbstractVDomView<CartItemScope> {

    private final CartItemSharedView shared;

    public CartItemView(StoreTeaVMApplication app, CartItemScope row) {
        super("cart-item-" + row.id.get(), app, row);

        this.shared = new CartItemSharedView();
        this.shared.stateSupplier = () -> row;
        this.shared.onModifyQuantity = qty -> safeAction("ModifyQty", () -> row.onModifyQuantity.run(qty));
        this.shared.onRemove = () -> safeAction("Remove item", row.onRemove::run);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
