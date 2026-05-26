package br.com.wdc.shopping.view.gluon;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import br.com.wdc.shopping.view.gluon.impl.CartViewGluon;
import br.com.wdc.shopping.view.gluon.impl.HomeViewGluon;
import br.com.wdc.shopping.view.gluon.impl.LoginViewGluon;
import br.com.wdc.shopping.view.gluon.impl.ProductViewGluon;
import br.com.wdc.shopping.view.gluon.impl.ProductsPanelViewGluon;
import br.com.wdc.shopping.view.gluon.impl.PurchasesPanelViewGluon;
import br.com.wdc.shopping.view.gluon.impl.ReceiptViewGluon;
import br.com.wdc.shopping.view.gluon.impl.RootViewGluon;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;

public class ShoppingGluonApplication extends ShoppingApplication {

    static {
        RootPresenter.createView = RootViewGluon::new;
        LoginPresenter.createView = LoginViewGluon::new;
        HomePresenter.createView = HomeViewGluon::new;
        CartPresenter.createView = CartViewGluon::new;
        ProductPresenter.createView = ProductViewGluon::new;
        ReceiptPresenter.createView = ReceiptViewGluon::new;
        ProductsPanelPresenter.createView = ProductsPanelViewGluon::new;
        PurchasesPanelPresenter.createView = PurchasesPanelViewGluon::new;
    }

    private static final long FRAME_INTERVAL_NS = 16_000_000L;

    private StackPane rootPane;
    private final Map<String, AbstractViewGluon<?>> dirtyViewMap = new HashMap<>();
    private final Map<String, Object> attributeMap = new ConcurrentHashMap<>();
    private boolean flushScheduled;

    @Override
    protected Map<Integer, CubePresenter> createPresenterMap() {
        return new ConcurrentHashMap<>();
    }

    @Override
    protected <T> T createDelegate(Class<T> repoInterface, T delegate) {
        return ProxyRepositoryWrapper.wrap(repoInterface, delegate, this::getSecurityContext);
    }

    public void setRootPane(StackPane rootPane) {
        this.rootPane = rootPane;
    }

    public StackPane getRootPane() {
        return this.rootPane;
    }

    public void start() {
        // No-op: flush is scheduled on-demand via markDirty
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
                if (view instanceof AbstractViewGluon<?> gluonView) {
                    this.markDirty(gluonView);
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

    @Override
    public void alertUnexpectedError(Log logger, String message, Throwable e) {
        if (logger != null) {
            logger.error(message, e);
        } else {
            Log.getLogger(ShoppingGluonApplication.class).error(message, e);
        }
        var alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Erro");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void markDirty(AbstractViewGluon<?> view) {
        if (Platform.isFxApplicationThread()) {
            this.dirtyViewMap.putIfAbsent(view.instanceId(), view);
            view.dirtyTimestamp = System.nanoTime();
            scheduleFlush();
        } else {
            Platform.runLater(() -> markDirty(view));
        }
    }

    private void scheduleFlush() {
        if (!this.flushScheduled) {
            this.flushScheduled = true;
            Platform.runLater(this::flushDirtyViews);
        }
    }

    private void flushDirtyViews() {
        this.flushScheduled = false;
        if (this.dirtyViewMap.isEmpty()) {
            return;
        }

        var nowNanos = System.nanoTime();
        var threshold = nowNanos - FRAME_INTERVAL_NS;
        var iterator = this.dirtyViewMap.values().iterator();
        while (iterator.hasNext()) {
            var view = iterator.next();
            if (view.dirtyTimestamp <= threshold) {
                iterator.remove();
                view.doUpdate();
            }
        }

        // Re-schedule if there are remaining views not yet due
        if (!this.dirtyViewMap.isEmpty()) {
            scheduleFlush();
        }
    }

    @Override
    public String b64Cipher(String text) {
        throw new AssertionError("Not implemented");
    }

    @Override
    public String b64Decipher(String b64Text) {
        throw new AssertionError("Not implemented");
    }
}
