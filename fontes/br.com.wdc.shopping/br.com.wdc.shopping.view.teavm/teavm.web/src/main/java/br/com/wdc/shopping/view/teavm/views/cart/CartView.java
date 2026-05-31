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
        this.shared.onBack = () -> safeAction("Back", presenter::onOpenProducts);
        this.shared.onBuy = () -> safeAction("Buy", presenter::onBuy);
        this.shared.onModifyQuantity = (id, qty) -> safeAction("ModifyQty", () -> presenter.onModifyQuantity(id, qty));
        this.shared.onRemoveProduct = id -> safeAction("Remove item", () -> presenter.onRemoveProduct(id));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
