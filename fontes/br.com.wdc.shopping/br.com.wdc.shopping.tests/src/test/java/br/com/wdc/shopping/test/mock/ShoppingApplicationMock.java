package br.com.wdc.shopping.test.mock;

import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartScope;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.test.mock.viewimpl.CartViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductsPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.PurchasesPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RootViewMock;

public class ShoppingApplicationMock extends ShoppingApplication {

    /** The views of the scopes that have no presenter of their own (segments, rows). */
    private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
            .register(CartScope.CartItemScope.class, (ShoppingApplicationMock app, CartScope.CartItemScope row) -> new CubeView() {
                @Override
                public String instanceId() {
                    return "cart-row-mock-" + row.id.get();
                }

                @Override
                public void update() {
                    // NOOP
                }

                @Override
                public void release() {
                    // NOOP
                }
            })
            .register(HomeScope.ProductsPanelScope.class, (ShoppingApplicationMock app, HomeScope.ProductsPanelScope s) -> new ProductsPanelViewMock(app, s))
            .register(HomeScope.PurchasesPanelScope.class, (ShoppingApplicationMock app, HomeScope.PurchasesPanelScope s) -> new PurchasesPanelViewMock(app, s));

    static {
        RootPresenter.createView = RootViewMock::new;
        LoginPresenter.createView = LoginViewMock::new;
        HomePresenter.createView = HomeViewMock::new;
        CartPresenter.createView = CartViewMock::new;
        ProductPresenter.createView = ProductViewMock::new;
        ReceiptPresenter.createView = ReceiptViewMock::new;
    }

    public ShoppingApplicationMock() {
        this.setScopeViews(SCOPE_VIEWS);
        // NOOP
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
        return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
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
    public void updateHistory() {
        // NOOP
    }

    @Override
    public String b64Cipher(String text) {
        throw new NotImplementedException();
    }

    @Override
    public String b64Decipher(String b64Text) {
        throw new NotImplementedException();
    }

    @Override
    public String getClientIp() {
        return "127.0.0.1";
    }

    private final InMemoryClientStorage sessionStore    = new InMemoryClientStorage();
    private final InMemoryClientStorage persistentStore = new InMemoryClientStorage();

    @Override
    public ClientStorage clientSessionStore()    { return sessionStore; }

    @Override
    public ClientStorage clientPersistentStore() { return persistentStore; }

}