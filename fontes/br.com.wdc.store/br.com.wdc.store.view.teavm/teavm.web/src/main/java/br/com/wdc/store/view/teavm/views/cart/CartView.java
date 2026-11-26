package br.com.wdc.store.view.teavm.views.cart;

import java.util.Objects;

import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.cart.CartSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;
import br.com.wdc.store.view.teavm.views.AbstractViewTeaVM;

public class CartView extends AbstractVDomView<CartScope> {

    private final CartSharedView shared;

    public CartView(StoreTeaVMApplication app, CartScope scope) {
        super("cart", app, scope);

        this.shared = new CartSharedView();
        this.shared.scopeSupplier = () -> scope;
        this.shared.onBack = () -> safeAction("Back", scope.onOpenProducts::run);
        this.shared.onBuy = () -> safeAction("Buy", scope.onBuy::run);
        // The rows are views of their own (CartItemView): the cart lays their elements out in its order
        this.shared.rowElements = () -> scope.rows.get().stream()
                .map(row -> this.viewOf(row) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null)
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
