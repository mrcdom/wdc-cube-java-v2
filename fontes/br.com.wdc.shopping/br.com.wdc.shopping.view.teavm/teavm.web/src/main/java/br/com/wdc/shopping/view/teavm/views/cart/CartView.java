package br.com.wdc.shopping.view.teavm.views.cart;

import java.util.Objects;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.cart.CartSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;

public class CartView extends AbstractVDomView<CartPresenter> {

    private final CartSharedView shared;

    public CartView(CartPresenter presenter) {
        super("cart", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new CartSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.onBack = () -> safeAction("Back", presenter.state.onOpenProducts::run);
        this.shared.onBuy = () -> safeAction("Buy", presenter.state.onBuy::run);
        // The rows are views of their own (CartItemView): the cart lays their elements out in its order
        this.shared.rowElements = () -> presenter.state.rows.get().stream()
                .map(row -> row.view() instanceof AbstractViewTeaVM<?> v ? v.getElement() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
