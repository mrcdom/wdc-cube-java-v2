package br.com.wdc.shopping.view.vaadin;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;

import br.com.wdc.framework.cube.AbstractCubePresenter;
import br.com.wdc.shopping.presentation.ShoppingApplication;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.vaadin.impl.CartViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.HomeViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.LoginViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.ProductViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.ProductsPanelViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.PurchasesPanelViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.ReceiptViewVaadin;
import br.com.wdc.shopping.view.vaadin.impl.RootViewVaadin;
import br.com.wdc.shopping.view.vaadin.util.IntentSigner;

public class ShoppingVaadinApplication extends ShoppingApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingVaadinApplication.class);

    private static final Map<String, ShoppingVaadinApplication> APP_CACHE = new ConcurrentHashMap<>();

    static {
        RootPresenter.createView = p -> new RootViewVaadin((ShoppingVaadinApplication) p.app, p);
        LoginPresenter.createView = p -> new LoginViewVaadin((ShoppingVaadinApplication) p.app, p);
        HomePresenter.createView = p -> new HomeViewVaadin((ShoppingVaadinApplication) p.app, p);
        CartPresenter.createView = p -> new CartViewVaadin((ShoppingVaadinApplication) p.app, p);
        ProductPresenter.createView = p -> new ProductViewVaadin((ShoppingVaadinApplication) p.app, p);
        ReceiptPresenter.createView = p -> new ReceiptViewVaadin((ShoppingVaadinApplication) p.app, p);
        ProductsPanelPresenter.createView = p -> new ProductsPanelViewVaadin((ShoppingVaadinApplication) p.app, p);
        PurchasesPanelPresenter.createView = p -> new PurchasesPanelViewVaadin((ShoppingVaadinApplication) p.app, p);
    }

    private UI ui;
    private Div rootContainer;
    private final Map<String, AbstractViewVaadin<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<>();
    private final AtomicBoolean navigatingFromBrowser = new AtomicBoolean(false);
    private final IntentSigner intentSigner = new IntentSigner();
    private String lastSignature;

    public ShoppingVaadinApplication(UI ui) {
        this.ui = ui;
    }

    public static ShoppingVaadinApplication restoreFromCache(String signedHash) {
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

    public static boolean isCached(ShoppingVaadinApplication app) {
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
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp && acp.view() instanceof AbstractViewVaadin<?> view) {
                view.recreate();
            }
        }
    }

    @Override
    public void updateHistory() {
        // Mark all active views as dirty for UI refresh
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewVaadin<?> vaadinView) {
                    this.markDirty(vaadinView);
                }
            }
        }

        // Sync browser URL hash with current MVP intent (signed)
        if (!navigatingFromBrowser.get()) {
            try {
                var intent = this.newIntent();
                var intentStr = intent.toString();
                this.fragment = intentStr;
                var signedUrl = intentSigner.sign(intentStr);
                var newSignature = intentSigner.extractSignature(signedUrl);
                // Update cache: remove old entry, add new
                if (lastSignature != null) {
                    APP_CACHE.remove(lastSignature);
                }
                lastSignature = newSignature;
                APP_CACHE.put(newSignature, this);
                Optional.ofNullable(this.ui).ifPresent(ui -> {
                    ui.access(() -> {
                        ui.getPage().executeJs(
                                "history.pushState(null, '', '#' + $0)", signedUrl);
                    });
                });
            } catch (Exception e) {
                LOG.error("Failed to push browser history state", e);
            }
        }
    }

    public void handleBrowserNavigation(String signedLocation) {
        if (!intentSigner.verify(signedLocation)) {
            LOG.warn("Rejected tampered URL: {}", signedLocation);
            // Re-push the current valid state to the browser
            if (this.fragment != null) {
                var signedUrl = intentSigner.sign(this.fragment);
                Optional.ofNullable(this.ui).ifPresent(ui -> {
                    ui.access(() -> {
                        ui.getPage().executeJs(
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
    public Object setAttribute(String name, Object value) {
        return this.attributeMap.put(name, value);
    }

    @Override
    public Object getAttribute(String name) {
        return this.attributeMap.get(name);
    }

    @Override
    public Object removeAttribute(String name) {
        return this.attributeMap.remove(name);
    }

    public void markDirty(AbstractViewVaadin<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            this.scheduleFlush();
        }
    }

    private void scheduleFlush() {
        Optional.ofNullable(this.ui).ifPresent(ui -> {
            ui.access(this::flushDirtyViews);
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
