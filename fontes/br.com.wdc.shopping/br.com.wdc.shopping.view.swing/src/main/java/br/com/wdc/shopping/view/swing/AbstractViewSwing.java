package br.com.wdc.shopping.view.swing;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewSwing<P> implements CubeView {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractViewSwing.class);

    protected final String instanceId;
    protected final ShoppingSwingApplication app;
    protected final P presenter;
    protected final JPanel element;

    long dirtyTimestamp;

    protected AbstractViewSwing(String instanceId, ShoppingSwingApplication app, P presenter, JPanel element) {
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

    public JPanel getElement() {
        return this.element;
    }

    public abstract void doUpdate();

    public void rebuild() {
        this.element.removeAll();
        this.onRebuild();
        this.update();
    }

    protected void onRebuild() {
        // Subclasses override to reset notRendered and cached state
    }

    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, context, caught);
        }
    }

    protected <T, V extends AbstractViewSwing<?>> BiConsumer<List<T>, List<V>> newListSlot(
            JPanel container, Supplier<V> factory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewSwing<?>> void syncListSlot(
            JPanel container, List<T> items, List<V> viewList,
            Supplier<V> factory, BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = viewList.size();

        // Remove excess views
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                var view = viewList.remove(i);
                container.remove(view.getElement());
            }
        }

        // Add missing views
        while (viewList.size() < newSize) {
            var view = factory.get();
            viewList.add(view);
            container.add(view.getElement());
        }

        // Update all
        for (int i = 0; i < newSize; i++) {
            updater.accept(viewList.get(i), items.get(i));
        }

        container.revalidate();
        container.repaint();
    }
}
