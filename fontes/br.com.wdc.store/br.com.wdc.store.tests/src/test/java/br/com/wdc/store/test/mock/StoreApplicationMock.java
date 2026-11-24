package br.com.wdc.store.test.mock;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.test.mock.viewimpl.CartViewMock;
import br.com.wdc.store.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.store.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductsPanelViewMock;
import br.com.wdc.store.test.mock.viewimpl.PurchasesPanelViewMock;
import br.com.wdc.store.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.store.test.mock.viewimpl.RootViewMock;

public class StoreApplicationMock extends StoreApplication {

    /** The view of each scope: the presenters', the segments' and the rows'. */
    private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
            .register(RootScope.class, RootViewMock::new)
            .register(LoginScope.class, LoginViewMock::new)
            .register(HomeScope.class, HomeViewMock::new)
            .register(ProductScope.class, ProductViewMock::new)
            .register(CartScope.class, CartViewMock::new)
            .register(ReceiptScope.class, ReceiptViewMock::new)
            .register(CartScope.CartItemScope.class, (StoreApplicationMock app, CartScope.CartItemScope row) -> new CubeView() {
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
            .register(HomeScope.ProductsPanelScope.class, (StoreApplicationMock app, HomeScope.ProductsPanelScope s) -> new ProductsPanelViewMock(app, s))
            .register(HomeScope.PurchasesPanelScope.class, (StoreApplicationMock app, HomeScope.PurchasesPanelScope s) -> new PurchasesPanelViewMock(app, s));

    public StoreApplicationMock() {
        this.setScopeViews(SCOPE_VIEWS);
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
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