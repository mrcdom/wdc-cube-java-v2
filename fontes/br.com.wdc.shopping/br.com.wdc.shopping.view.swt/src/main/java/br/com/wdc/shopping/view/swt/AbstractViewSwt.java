package br.com.wdc.shopping.view.swt;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
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

    protected AbstractViewSwt(String instanceId, ShoppingSwtApplication app, P presenter) {
        this(instanceId, app, presenter, new Composite(app.getOffscreen(), SWT.NONE));
    }

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

    protected <T, V extends Composite> void syncList(
            Composite container, List<T> items, List<V> slots,
            Supplier<V> factory, BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = slots.size();

        // Remove excess slots
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                var slot = slots.remove(i);
                slot.dispose();
            }
        }

        // Add missing slots
        while (slots.size() < newSize) {
            var slot = factory.get();
            slots.add(slot);
        }

        // Update all
        for (int i = 0; i < newSize; i++) {
            updater.accept(slots.get(i), items.get(i));
        }

        container.layout(true, true);
    }
}
