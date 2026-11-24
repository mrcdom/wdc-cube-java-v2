package br.com.wdc.store.view.vaadin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.commons.storage.ClientStorage;
import br.com.wdc.framework.commons.storage.InMemoryClientStorage;
import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.ScopeViews;
import br.com.wdc.store.presentation.StoreApplication;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.view.vaadin.impl.CartViewVaadin;
import br.com.wdc.store.view.vaadin.impl.HomeViewVaadin;
import br.com.wdc.store.view.vaadin.impl.LoginViewVaadin;
import br.com.wdc.store.view.vaadin.impl.ProductViewVaadin;
import br.com.wdc.store.view.vaadin.impl.ProductsPanelViewVaadin;
import br.com.wdc.store.view.vaadin.impl.PurchasesPanelViewVaadin;
import br.com.wdc.store.view.vaadin.impl.ReceiptViewVaadin;
import br.com.wdc.store.view.vaadin.impl.RootViewVaadin;
import br.com.wdc.store.view.vaadin.impl.cart.CartItemViewVaadin;
import br.com.wdc.store.view.vaadin.util.IntentSigner;

public class StoreVaadinApplication extends StoreApplication {

    private static final Log LOG = Log.getLogger(StoreVaadinApplication.class);

    private static final Map<String, StoreVaadinApplication> APP_CACHE = new ConcurrentHashMap<>();

    /** The view of each scope: the presenters', the segments' and the rows'. */
    private static final ScopeViews SCOPE_VIEWS = new ScopeViews()
            .register(RootScope.class, RootViewVaadin::new)
            .register(LoginScope.class, LoginViewVaadin::new)
            .register(HomeScope.class, HomeViewVaadin::new)
            .register(ProductScope.class, ProductViewVaadin::new)
            .register(CartScope.class, CartViewVaadin::new)
            .register(ReceiptScope.class, ReceiptViewVaadin::new)
            .register(CartScope.CartItemScope.class, (StoreVaadinApplication app, CartScope.CartItemScope row) -> new CartItemViewVaadin(app, row))
            .register(HomeScope.ProductsPanelScope.class, (StoreVaadinApplication app, HomeScope.ProductsPanelScope s) -> new ProductsPanelViewVaadin(app, s))
            .register(HomeScope.PurchasesPanelScope.class, (StoreVaadinApplication app, HomeScope.PurchasesPanelScope s) -> new PurchasesPanelViewVaadin(app, s));

    private UI ui;
    private Div rootContainer;
    private final Map<String, AbstractViewVaadin<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private final AtomicBoolean navigatingFromBrowser = new AtomicBoolean(false);
    private final IntentSigner intentSigner = new IntentSigner();
    private String lastSignature;

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected Map<String, Object> createAttributeMap() {
        return new ConcurrentHashMap<>();
    }

    public StoreVaadinApplication(UI ui) {
        this.setScopeViews(SCOPE_VIEWS);
        this.ui = ui;
    }

    public static StoreVaadinApplication restoreFromCache(String signedHash) {
        var signIdx = signedHash.lastIndexOf("sign=");
        if (signIdx < 0) {
            return null;
        }
        var sign = signedHash.substring(signIdx + 5);
        var cached = APP_CACHE.remove(sign);
        if (cached != null) {
            LOG.info("Restored application from cache");
        }
        return cached;
    }

    public static boolean isCached(StoreVaadinApplication app) {
        return app.lastSignature != null && APP_CACHE.containsValue(app);
    }

    public UI getUI() {
        return this.ui;
    }

    public IntentSigner getIntentSigner() {
        return this.intentSigner;
    }

    public void setRootContainer(Div rootContainer) {
        this.rootContainer = rootContainer;
    }

    public Div getRootContainer() {
        return this.rootContainer;
    }

    @Override
    public void release() {
        // Remove from cache on release
        if (lastSignature != null) {
            APP_CACHE.remove(lastSignature);
            lastSignature = null;
        }
        this.dirtyViewMap.clear();
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            rootPresenter.release();
        }
    }

    public void reattach(UI newUi, Div newRootContainer) {
        this.ui = newUi;
        this.rootContainer = newRootContainer;
        this.dirtyViewMap.clear();

        // Recreate Vaadin components in all views (old components are tied to destroyed UI)
        // Presenters and application state (subject, securityContext, cart) are preserved
        for (var view : this.liveViews()) {
            if (view instanceof AbstractViewVaadin<?> vaadinView) {
                vaadinView.recreate();
            }
        }
    }

    @Override
    public void updateHistory() {
        // The host draws the root presenter's scope; each view asks for the views of the scopes it holds
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            this.viewOf(rootPresenter.scope);
        }

        // Mark all active views as dirty for UI refresh
        for (var view : this.liveViews()) {
            if (view instanceof AbstractViewVaadin<?> vaadinView) {
                this.markDirty(vaadinView);
            }
        }

        // Sync browser URL hash with current MVP intent (signed)
        try {
            var intent = this.newIntent();
            var intentStr = intent.toString();
            if (intentStr.equals(this.fragment)) {
                // Every action republishes the address; only a change becomes a history entry
                return;
            }
            this.fragment = intentStr;
            if (!navigatingFromBrowser.get()) {
                var signedUrl = intentSigner.sign(intentStr);
                var newSignature = intentSigner.extractSignature(signedUrl);
                // Update cache: remove old entry, add new
                if (lastSignature != null) {
                    APP_CACHE.remove(lastSignature);
                }
                lastSignature = newSignature;
                APP_CACHE.put(newSignature, this);
                Optional.ofNullable(this.ui).ifPresent(currentUi -> {
                    currentUi.access(() -> {
                        currentUi.getPage().executeJs(
                                "history.pushState(null, '', '#' + $0)", signedUrl);
                    });
                });
            }
        } catch (Exception e) {
            LOG.error("Failed to push browser history state", e);
        }
    }

    public void handleBrowserNavigation(String signedLocation) {
        if (!intentSigner.verify(signedLocation)) {
            LOG.warn("Rejected tampered URL: {}", signedLocation);
            // Re-push the current valid state to the browser
            if (this.fragment != null) {
                var signedUrl = intentSigner.sign(this.fragment);
                Optional.ofNullable(this.ui).ifPresent(currentUi -> {
                    currentUi.access(() -> {
                        currentUi.getPage().executeJs(
                                "history.replaceState(null, '', '#' + $0)", signedUrl);
                    });
                });
            }
            return;
        }

        var location = intentSigner.stripSignature(signedLocation);
        this.navigatingFromBrowser.set(true);
        try {
            this.go(location);
        } catch (Exception e) {
            LOG.error("Failed to handle browser navigation to: {}", location, e);
        } finally {
            this.navigatingFromBrowser.set(false);
        }
    }

    @Override
    public String b64Cipher(String text) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String b64Decipher(String b64Text) {
        throw new AssertionError("not implemented");
    }

    @Override
    public String getClientIp() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (request == null) {
            return "127.0.0.1";
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private final InMemoryClientStorage sessionStore    = new InMemoryClientStorage();
    private final InMemoryClientStorage persistentStore = new InMemoryClientStorage();

    @Override
    public ClientStorage clientSessionStore()    { return sessionStore; }

    @Override
    public ClientStorage clientPersistentStore() { return persistentStore; }

    public void markDirty(AbstractViewVaadin<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            this.scheduleFlush();
        }
    }

    private void scheduleFlush() {
        Optional.ofNullable(this.ui).ifPresent(currentUi -> {
            currentUi.access(this::flushDirtyViews);
        });
    }

    private void flushDirtyViews() {
        var iterator = this.dirtyViewMap.values().iterator();
        while (iterator.hasNext()) {
            var view = iterator.next();
            iterator.remove();
            view.doUpdate();
        }
    }
}
