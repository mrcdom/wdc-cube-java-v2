package br.com.wdc.shopping.view.gluon;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public abstract class AbstractViewGluon<P> implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewGluon.class);

    protected final String instanceId;
    protected final ShoppingGluonApplication app;
    protected final P presenter;
    protected final Parent element;

    long dirtyTimestamp;

    protected AbstractViewGluon(String instanceId, ShoppingGluonApplication app, P presenter, Parent element) {
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

    public Parent getElement() {
        return this.element;
    }

    public abstract void doUpdate();

    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, context, caught);
        }
    }

    protected <T, V extends AbstractViewGluon<?>> BiConsumer<List<T>, List<V>> newListSlot(
            Pane container, Supplier<V> factory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewGluon<?>> void syncListSlot(
            Pane container, List<T> items, List<V> viewList,
            Supplier<V> factory, BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = viewList.size();

        if (oldSize > newSize) {
            viewList.subList(newSize, oldSize).clear();
            container.getChildren().remove(newSize, container.getChildren().size());
        }

        while (viewList.size() < newSize) {
            var view = factory.get();
            viewList.add(view);
            container.getChildren().add(view.getElement());
        }

        for (int i = 0; i < newSize; i++) {
            updater.accept(viewList.get(i), items.get(i));
        }
    }
}
