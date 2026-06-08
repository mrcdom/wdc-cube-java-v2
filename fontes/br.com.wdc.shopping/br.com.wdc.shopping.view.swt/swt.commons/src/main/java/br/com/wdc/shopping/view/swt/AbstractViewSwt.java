package br.com.wdc.shopping.view.swt;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewSwt implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewSwt.class);

    protected final String instanceId;
    protected final SwtApp app;
    protected final Composite element;

    long dirtyTimestamp;
    private boolean needsInitialLayout = true;

    protected AbstractViewSwt(String instanceId, SwtApp app) {
        this(instanceId, app, new Composite(app.getOffscreen(), SWT.NONE));
    }

    protected AbstractViewSwt(String instanceId, SwtApp app, Composite element) {
        this.instanceId = instanceId;
        this.app = app;
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

    /**
     * Called by the render loop. Delegates to doUpdate() and forces layout on first render
     * so that children get their dimensions (triggering paint/resize events).
     */
    public void performUpdate() {
        doUpdate();
        if (this.needsInitialLayout) {
            this.needsInitialLayout = false;
            this.element.layout(true, true);
        }
    }

    public void rebuild() {
        for (var child : this.element.getChildren()) {
            child.dispose();
        }
        this.needsInitialLayout = true;
        this.onRebuild();
        this.update();
    }

    protected void onRebuild() {
        // Subclasses override to reset notRendered and cached state
    }

    protected void safeAction(String context, Runnable action) {
        this.app.runAction(() -> {
            try {
                action.run();
            } catch (Exception caught) {
                LOG.error("Action '{}' failed: {}", context, caught.getMessage(), caught);
                this.app.onActionError(context, caught);
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
