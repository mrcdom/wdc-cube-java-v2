package br.com.wdc.shopping.view.swt;

import org.eclipse.swt.widgets.Composite;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewSwt<P> implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewSwt.class);

    protected final String instanceId;
    protected final ShoppingSwtApplication app;
    protected final P presenter;
    protected final Composite element;

    long dirtyTimestamp;

    protected AbstractViewSwt(String instanceId, ShoppingSwtApplication app, P presenter, Composite element) {
        this.instanceId = instanceId;
        this.app = app;
        this.presenter = presenter;
        this.element = element;
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
        // NOOP - subclasses can override
    }

    public Composite getElement() {
        return this.element;
    }

    public abstract void doUpdate();

    public void rebuild() {
        for (var child : this.element.getChildren()) {
            child.dispose();
        }
        this.onRebuild();
        this.update();
    }

    protected void onRebuild() {
        // Subclasses override to reset notRendered and cached state
    }

    protected void safeAction(String context, Runnable action) {
        this.app.runPresenterAction(() -> {
            try {
                action.run();
            } catch (Exception caught) {
                this.app.alertUnexpectedError(LOG, context, caught);
            }
        });
    }
}
