package br.com.wdc.shopping.view.teavm.views.cart;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.cart.CartSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class CartView extends AbstractVDomView<CartPresenter> {

    private final CartSharedView shared;

    public CartView(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new CartSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.onBack = () -> safeAction("Back", presenter.state.onOpenProducts::run);
        this.shared.onBuy = () -> safeAction("Buy", presenter.state.onBuy::run);
        this.shared.onModifyQuantity = (id, qty) -> safeAction("ModifyQty", () -> presenter.state.onModifyQuantity.run(id, qty));
        this.shared.onRemoveProduct = id -> safeAction("Remove item", () -> presenter.state.onRemoveProduct.run(id));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
