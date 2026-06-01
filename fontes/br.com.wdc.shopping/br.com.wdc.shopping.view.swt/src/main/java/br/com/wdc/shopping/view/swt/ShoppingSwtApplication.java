package br.com.wdc.shopping.view.swt;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.AbstractCubePresenter;
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
import br.com.wdc.shopping.view.swt.impl.LoginViewSwt;
import br.com.wdc.shopping.view.swt.impl.RootViewSwt;

public class ShoppingSwtApplication extends ShoppingApplication {

    private static final Log LOG = Log.getLogger(ShoppingSwtApplication.class);

    static {
        RootPresenter.createView = RootViewSwt::new;
        LoginPresenter.createView = LoginViewSwt::new;
        // TODO: implement remaining views
        HomePresenter.createView = p -> { throw new UnsupportedOperationException("HomeView not yet implemented"); };
        CartPresenter.createView = p -> { throw new UnsupportedOperationException("CartView not yet implemented"); };
        ProductPresenter.createView = p -> { throw new UnsupportedOperationException("ProductView not yet implemented"); };
        ReceiptPresenter.createView = p -> { throw new UnsupportedOperationException("ReceiptView not yet implemented"); };
        ProductsPanelPresenter.createView = p -> { throw new UnsupportedOperationException("ProductsPanelView not yet implemented"); };
        PurchasesPanelPresenter.createView = p -> { throw new UnsupportedOperationException("PurchasesPanelView not yet implemented"); };
    }

    private static final int FRAME_INTERVAL_MS = 16; // ~60fps

    private final Display display;
    private final Shell shell;
    private Composite rootPane;
    private final Map<String, AbstractViewSwt<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private Runnable renderTimerRunnable;
    private boolean devMode;

    public ShoppingSwtApplication(Display display, Shell shell) {
        this.display = display;
        this.shell = shell;
    }

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected Map<String, Object> createAttributeMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
        return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
    }

    public boolean isDevMode() {
        return this.devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public Display getDisplay() {
        return this.display;
    }

    public Shell getShell() {
        return this.shell;
    }

    public Composite getRootPane() {
        return this.rootPane;
    }

    public void setRootPane(Composite rootPane) {
        this.rootPane = rootPane;
    }

    public void start() {
        this.renderTimerRunnable = () -> {
            if (!this.display.isDisposed()) {
                this.flushDirtyViews();
                this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable);
            }
        };
        this.display.timerExec(FRAME_INTERVAL_MS, this.renderTimerRunnable);
    }

    @Override
    public void release() {
        this.renderTimerRunnable = null;
        this.dirtyViewMap.clear();
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            rootPresenter.release();
        }
    }

    @Override
    public void updateHistory() {
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewSwt<?> swtView) {
                    this.markDirty(swtView);
                }
            }
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

    public void markDirty(AbstractViewSwt<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            view.dirtyTimestamp = System.nanoTime();
        }
    }

    public void rebuildAllViews() {
        LOG.info("Rebuilding all SWT views...");
        this.dirtyViewMap.clear();
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewSwt<?> swtView) {
                    swtView.rebuild();
                }
            }
        }
        if (this.rootPane != null && !this.rootPane.isDisposed()) {
            this.rootPane.layout(true, true);
        }
        LOG.info("All SWT views rebuilt.");
    }

    private void flushDirtyViews() {
        if (this.dirtyViewMap.isEmpty()) {
            return;
        }

        var threshold = System.nanoTime() - (FRAME_INTERVAL_MS * 1_000_000L);
        var iterator = this.dirtyViewMap.values().iterator();
        while (iterator.hasNext()) {
            var view = iterator.next();
            if (view.dirtyTimestamp <= threshold) {
                iterator.remove();
                view.doUpdate();
            }
        }
    }
}
