package br.com.wdc.store.test.mock;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.NotImplementedException;

import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.test.mock.viewimpl.AbstractViewMock;
import br.com.wdc.store.test.mock.viewimpl.CartItemViewMock;
import br.com.wdc.store.test.mock.viewimpl.CartViewMock;
import br.com.wdc.store.test.mock.viewimpl.HomeViewMock;
import br.com.wdc.store.test.mock.viewimpl.LoginViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductViewMock;
import br.com.wdc.store.test.mock.viewimpl.ProductsPanelViewMock;
import br.com.wdc.store.test.mock.viewimpl.PurchasesPanelViewMock;
import br.com.wdc.store.test.mock.viewimpl.ReceiptViewMock;
import br.com.wdc.store.test.mock.viewimpl.RootViewMock;

public class StoreApplicationMock extends StoreApplication {

    /** The view of each scope class: the presenters', the segments' and the rows'. */
    private static final Map<Class<?>, BiFunction<StoreApplicationMock, Scope, AbstractViewMock<?>>> VIEWS = Map.of(
            RootScope.class, (app, s) -> new RootViewMock(app, (RootScope) s),
            LoginScope.class, (app, s) -> new LoginViewMock(app, (LoginScope) s),
            HomeScope.class, (app, s) -> new HomeViewMock(app, (HomeScope) s),
            ProductScope.class, (app, s) -> new ProductViewMock(app, (ProductScope) s),
            CartScope.class, (app, s) -> new CartViewMock(app, (CartScope) s),
            ReceiptScope.class, (app, s) -> new ReceiptViewMock(app, (ReceiptScope) s),
            CartScope.CartItemScope.class, (app, s) -> new CartItemViewMock(app, (CartScope.CartItemScope) s),
            HomeScope.ProductsPanelScope.class, (app, s) -> new ProductsPanelViewMock(app, (HomeScope.ProductsPanelScope) s),
            HomeScope.PurchasesPanelScope.class, (app, s) -> new PurchasesPanelViewMock(app, (HomeScope.PurchasesPanelScope) s));

    /** The views made so far; weak on the scope, so a scope that went away takes its view with it. */
    private final Map<Scope, AbstractViewMock<?>> views = Collections.synchronizedMap(new WeakHashMap<>());

    /** The view that draws the scope, made the first time a test asks for it; {@code null} for no scope. */
    public AbstractViewMock<?> viewOf(Scope scope) {
        if (scope == null) {
            return null;
        }
        return this.views.computeIfAbsent(scope, s -> {
            var factory = VIEWS.get(s.getClass());
            if (factory == null) {
                throw new IllegalStateException("No view mock for scope " + s.getClass().getName());
            }
            return factory.apply(this, s);
        });
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    /** The tests start from the root presenter's scope; every other view is asked for from the scope that holds it. */
    public RootViewMock getRootView() {
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null && this.viewOf(rootPresenter.scope) instanceof RootViewMock rootView) {
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