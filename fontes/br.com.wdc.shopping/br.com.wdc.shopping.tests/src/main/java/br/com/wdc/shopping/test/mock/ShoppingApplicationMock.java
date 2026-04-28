package br.com.wdc.shopping.test.mock;

import java.util.HashMap;
import java.util.Map;

import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.test.mock.viewimpl.CartViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductsPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.PurchasesPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RestrictedViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RootViewMock;

public class ShoppingApplicationMock extends ShoppingApplication {

    static {
        RootPresenter.createView = p -> new RootViewMock((ShoppingApplicationMock) p.app, p);
        LoginPresenter.createView = p -> new LoginViewMock((ShoppingApplicationMock) p.app, p);
        HomePresenter.createView = p -> new RestrictedViewMock((ShoppingApplicationMock) p.app, p);
        CartPresenter.createView = p -> new CartViewMock((ShoppingApplicationMock) p.app, p);
        ProductPresenter.createView = p -> new ProductViewMock((ShoppingApplicationMock) p.app, p);
        ReceiptPresenter.createView = p -> new ReceiptViewMock((ShoppingApplicationMock) p.app, p);
        ProductsPanelPresenter.createView = ProductsPanelViewMock::new;
        PurchasesPanelPresenter.createView = PurchasesPanelViewMock::new;
    }

    private final Map<String, Object> attributes = new HashMap<>();

    public ShoppingApplicationMock() {
        // NOOP
    }

    public RootViewMock getRootView() {
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null && rootPresenter.view() instanceof RootViewMock rootView) {
            return rootView;
        } else {
            return null;
        }
    }

    @Override
    public Object setAttribute(String name, Object value) {
        return this.attributes.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributes.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributes.remove(name);
    }

    @Override
    public void updateHistory() {
        // NOOP
    }

}