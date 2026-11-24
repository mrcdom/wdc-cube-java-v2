package br.com.wdc.store.test.mock.viewimpl;

import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class CartItemViewMock extends AbstractViewMock<CartScope.CartItemScope> {

    public CartItemViewMock(StoreApplicationMock app, CartScope.CartItemScope scope) {
        super(app, scope);
    }

}
