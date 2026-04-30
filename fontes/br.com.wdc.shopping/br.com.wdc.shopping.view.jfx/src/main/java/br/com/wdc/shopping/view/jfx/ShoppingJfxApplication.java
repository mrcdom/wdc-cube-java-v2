package br.com.wdc.shopping.view.jfx;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import br.com.wdc.shopping.view.jfx.impl.CartViewJfx;
import br.com.wdc.shopping.view.jfx.impl.HomeViewJfx;
import br.com.wdc.shopping.view.jfx.impl.LoginViewJfx;
import br.com.wdc.shopping.view.jfx.impl.ProductViewJfx;
import br.com.wdc.shopping.view.jfx.impl.ProductsPanelViewJfx;
import br.com.wdc.shopping.view.jfx.impl.PurchasesPanelViewJfx;
import br.com.wdc.shopping.view.jfx.impl.ReceiptViewJfx;
import br.com.wdc.shopping.view.jfx.impl.RootViewJfx;
import javafx.animation.AnimationTimer;
import javafx.scene.layout.StackPane;

public class ShoppingJfxApplication extends ShoppingApplication {

    static {
        RootPresenter.createView = p -> new RootViewJfx((ShoppingJfxApplication) p.app, p);
        LoginPresenter.createView = p -> new LoginViewJfx((ShoppingJfxApplication) p.app, p);
        HomePresenter.createView = p -> new HomeViewJfx((ShoppingJfxApplication) p.app, p);
        CartPresenter.createView = p -> new CartViewJfx((ShoppingJfxApplication) p.app, p);
        ProductPresenter.createView = p -> new ProductViewJfx((ShoppingJfxApplication) p.app, p);
        ReceiptPresenter.createView = p -> new ReceiptViewJfx((ShoppingJfxApplication) p.app, p);
        ProductsPanelPresenter.createView = p -> new ProductsPanelViewJfx((ShoppingJfxApplication) p.app, p);
        PurchasesPanelPresenter.createView = p -> new PurchasesPanelViewJfx((ShoppingJfxApplication) p.app, p);
    }

    private static final long FRAME_INTERVAL_NS = 16_000_000L; // 16ms

    private StackPane rootPane;
    private final Map<String, AbstractViewJfx<?>> dirtyViewMap = new ConcurrentHashMap<>();
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<>();
    private AnimationTimer renderLoop;

    public void setRootPane(StackPane rootPane) {
        this.rootPane = rootPane;
    }

    public StackPane getRootPane() {
        return this.rootPane;
    }

    public void start() {
        this.renderLoop = new AnimationTimer() {
            @Override
            public void handle(long nowNanos) {
                ShoppingJfxApplication.this.flushDirtyViews(nowNanos);
            }
        };
        this.renderLoop.start();
    }

    @Override
    public void release() {
        if (this.renderLoop != null) {
            this.renderLoop.stop();
            this.renderLoop = null;
        }
        this.dirtyViewMap.clear();
        var rootPresenter = this.getRootPresenter();
        if (rootPresenter != null) {
            rootPresenter.release();
        }
    }

    @Override
    public void updateHistory() {
        // Mark all active views as dirty so they render in the next cycle
        for (var presenter : this.presenterMap.values()) {
            if (presenter instanceof AbstractCubePresenter<?> acp) {
                var view = acp.view();
                if (view instanceof AbstractViewJfx<?> jfxView) {
                    this.markDirty(jfxView);
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

    public void markDirty(AbstractViewJfx<?> view) {
        if (this.dirtyViewMap.putIfAbsent(view.instanceId(), view) == null) {
            view.dirtyTimestamp = System.nanoTime();
        }
    }

    private void flushDirtyViews(long nowNanos) {
        if (this.dirtyViewMap.isEmpty()) {
            return;
        }

        var threshold = nowNanos - FRAME_INTERVAL_NS;
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
