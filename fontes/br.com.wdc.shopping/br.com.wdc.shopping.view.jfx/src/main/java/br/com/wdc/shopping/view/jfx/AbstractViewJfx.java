package br.com.wdc.shopping.view.jfx;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import br.com.wdc.framework.cube.CubeView;
import javafx.scene.Parent;
import javafx.scene.layout.Pane;

public abstract class AbstractViewJfx<P> implements CubeView {

    protected final String instanceId;
    protected final ShoppingJfxApplication app;
    protected final P presenter;
    protected final Parent element;

    long dirtyTimestamp;

    protected AbstractViewJfx(String instanceId, ShoppingJfxApplication app, P presenter, Parent element) {
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

    protected <T, V extends AbstractViewJfx<?>> BiConsumer<List<T>, List<V>> newListSlot(
            Pane container, Supplier<V> factory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewJfx<?>> void syncListSlot(
            Pane container, List<T> items, List<V> viewList,
            Supplier<V> factory, BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = viewList.size();

        // Remove excess views
        if (oldSize > newSize) {
            viewList.subList(newSize, oldSize).clear();
            container.getChildren().remove(newSize, container.getChildren().size());
        }

        // Add missing views
        while (viewList.size() < newSize) {
            var view = factory.get();
            viewList.add(view);
            container.getChildren().add(view.getElement());
        }

        // Update all
        for (int i = 0; i < newSize; i++) {
            updater.accept(viewList.get(i), items.get(i));
        }
    }
}
