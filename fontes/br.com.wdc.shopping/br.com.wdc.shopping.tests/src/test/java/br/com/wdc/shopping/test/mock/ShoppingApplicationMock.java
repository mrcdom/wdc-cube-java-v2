package br.com.wdc.shopping.test.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.shopping.presentation.ProxyRepositoryWrapper;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootScope;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginScope;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartScope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductScope;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptScope;
import br.com.wdc.shopping.test.mock.viewimpl.CartViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ProductsPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.PurchasesPanelViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RootViewMock;

public class ShoppingApplicationMock extends ShoppingApplication {

    /** The view of each scope: the presenters', the segments' and the rows'. */
    private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
            .register(RootScope.class, RootViewMock::new)
            .register(LoginScope.class, LoginViewMock::new)
            .register(HomeScope.class, HomeViewMock::new)
            .register(ProductScope.class, ProductViewMock::new)
            .register(CartScope.class, CartViewMock::new)
            .register(ReceiptScope.class, ReceiptViewMock::new)
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

    public ShoppingApplicationMock() {
        this.setScopeViews(SCOPE_VIEWS);
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
        return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
    }

    /** The host draws the root presenter's scope; every other view is asked for by the view that holds it. */
    public RootViewMock getRootView() {
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null && this.viewOf(rootPresenter.state) instanceof RootViewMock rootView) {
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