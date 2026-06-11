package br.com.wdc.shopping.test.mock;

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
import br.com.wdc.shopping.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.shopping.test.mock.viewimpl.RootViewMock;

public class ShoppingApplicationMock extends ShoppingApplication {

    static {
        RootPresenter.createView = RootViewMock::new;
        LoginPresenter.createView = LoginViewMock::new;
        HomePresenter.createView = HomeViewMock::new;
        CartPresenter.createView = CartViewMock::new;
        ProductPresenter.createView = ProductViewMock::new;
        ReceiptPresenter.createView = ReceiptViewMock::new;
        ProductsPanelPresenter.createView = ProductsPanelViewMock::new;
        PurchasesPanelPresenter.createView = PurchasesPanelViewMock::new;
    }

    public ShoppingApplicationMock() {
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