package br.com.wdc.shopping.view.swing;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JPanel;
import javax.swing.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import br.com.wdc.shopping.view.swing.impl.CartViewSwing;
import br.com.wdc.shopping.view.swing.impl.HomeViewSwing;
import br.com.wdc.shopping.view.swing.impl.LoginViewSwing;
import br.com.wdc.shopping.view.swing.impl.ProductViewSwing;
import br.com.wdc.shopping.view.swing.impl.ProductsPanelViewSwing;
import br.com.wdc.shopping.view.swing.impl.PurchasesPanelViewSwing;
import br.com.wdc.shopping.view.swing.impl.ReceiptViewSwing;
import br.com.wdc.shopping.view.swing.impl.RootViewSwing;
import br.com.wdc.shopping.view.swing.util.StackPanel;

public class ShoppingSwingApplication extends ShoppingApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingSwingApplication.class);

    static {
        RootPresenter.createView = p -> new RootViewSwing((ShoppingSwingApplication) p.app, p);
        LoginPresenter.createView = p -> new LoginViewSwing((ShoppingSwingApplication) p.app, p);
        HomePresenter.createView = p -> new HomeViewSwing((ShoppingSwingApplication) p.app, p);
        CartPresenter.createView = p -> new CartViewSwing((ShoppingSwingApplication) p.app, p);
        ProductPresenter.createView = p -> new ProductViewSwing((ShoppingSwingApplication) p.app, p);
        ReceiptPresenter.createView = p -> new ReceiptViewSwing((ShoppingSwingApplication) p.app, p);
        ProductsPanelPresenter.createView = p -> new ProductsPanelViewSwing((ShoppingSwingApplication) p.app, p);
        PurchasesPanelPresenter.createView = p -> new PurchasesPanelViewSwing((ShoppingSwingApplication) p.app, p);
    }

    private static final int FRAME_INTERVAL_MS = 16; // ~60fps

    private StackPanel rootPane;
    private final Map<String, AbstractViewSwing<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<>();
    private Timer renderTimer;
    private boolean devMode;

    public boolean isDevMode() {
        return this.devMode;
    }

    public void setDevMode(boolean devMode) {
        this.devMode = devMode;
    }

    public void setRootPane(StackPanel rootPane) {
        this.rootPane = rootPane;
    }

    public StackPanel getRootPane() {
        return this.rootPane;
    }

    public void start() {
        this.renderTimer = new Timer(FRAME_INTERVAL_MS, _ -> this.flushDirtyViews());
        this.renderTimer.start();
    }

    @Override
    public void release() {
        if (this.renderTimer != null) {
            this.renderTimer.stop();
            this.renderTimer = null;
        }
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
                if (view instanceof AbstractViewSwing<?> swingView) {
                    this.markDirty(swingView);
                }
            }
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

    public void markDirty(AbstractViewSwing<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            view.dirtyTimestamp = System.nanoTime();
        }
    }

    public void rebuildAllViews() {
        LOG.info("Rebuilding all Swing views...");
        this.dirtyViewMap.clear();
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewSwing<?> swingView) {
                    swingView.rebuild();
                }
            }
        }
        this.rootPane.revalidate();
        this.rootPane.repaint();
        LOG.info("All Swing views rebuilt.");
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
