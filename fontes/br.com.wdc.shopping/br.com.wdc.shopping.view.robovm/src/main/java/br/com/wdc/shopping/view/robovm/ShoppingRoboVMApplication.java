package br.com.wdc.shopping.view.robovm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIWindow;
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
import br.com.wdc.shopping.view.robovm.impl.CartViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.HomeViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.LoginViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.ProductViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.ProductsPanelViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.PurchasesPanelViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.ReceiptViewRoboVM;
import br.com.wdc.shopping.view.robovm.impl.RootViewRoboVM;

public class ShoppingRoboVMApplication extends ShoppingApplication {

    private static final Logger LOG = LoggerFactory.getLogger(ShoppingRoboVMApplication.class);

    static {
        RootPresenter.createView = p -> new RootViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        LoginPresenter.createView = p -> new LoginViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        HomePresenter.createView = p -> new HomeViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        CartPresenter.createView = p -> new CartViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        ProductPresenter.createView = p -> new ProductViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        ReceiptPresenter.createView = p -> new ReceiptViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        ProductsPanelPresenter.createView = p -> new ProductsPanelViewRoboVM((ShoppingRoboVMApplication) p.app, p);
        PurchasesPanelPresenter.createView = p -> new PurchasesPanelViewRoboVM((ShoppingRoboVMApplication) p.app, p);
    }

    private final Map<String, AbstractViewRoboVM<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<>();
    private UINavigationController rootController;
    private UIWindow window;

    public UINavigationController getRootController() {
        return rootController;
    }

    public void setRootController(UINavigationController rootController) {
        this.rootController = rootController;
    }

    public UIWindow getWindow() {
        return window;
    }

    public void setWindow(UIWindow window) {
        this.window = window;
    }

    @Override
    public void release() {
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
                if (view instanceof AbstractViewRoboVM<?> robovmView) {
                    this.markDirty(robovmView);
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

    public void markDirty(AbstractViewRoboVM<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            view.dirtyTimestamp = System.nanoTime();
        }
    }

    void flushDirtyViews() {
        if (this.dirtyViewMap.isEmpty()) {
            return;
        }

        var threshold = System.nanoTime() - (RoboVMRenderLoop.FRAME_INTERVAL_MS * 1_000_000L);
        var iterator = this.dirtyViewMap.values().iterator();
        while (iterator.hasNext()) {
            var view = iterator.next();
            if (view.dirtyTimestamp <= threshold) {
                iterator.remove();
                try {
                    view.doUpdate();
                } catch (Exception e) {
                    LOG.error("Error updating view: {}", view.instanceId(), e);
                }
            }
        }
    }
}
