package br.com.wdc.shopping.view.robovm;

import org.robovm.apple.uikit.UIView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewRoboVM<P> implements CubeView {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractViewRoboVM.class);

    protected final String instanceId;
    protected final ShoppingRoboVMApplication app;
    protected final P presenter;
    protected UIView rootView;

    long dirtyTimestamp;

    protected AbstractViewRoboVM(String instanceId, ShoppingRoboVMApplication app, P presenter) {
        this.instanceId = instanceId;
        this.app = app;
        this.presenter = presenter;
    }

    @Override
    public String instanceId() {
        return this.instanceId;
    }

    @Override
    public void update() {
        this.app.markDirty(this);
    }

    @Override
    public void release() {
        // Subclasses can override
    }

    public UIView getRootView() {
        return this.rootView;
    }

    public abstract void doUpdate();

    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, context, caught);
        }
    }
}
