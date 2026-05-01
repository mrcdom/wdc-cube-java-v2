package br.com.wdc.shopping.view.vaadin;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;

import br.com.wdc.framework.cube.CubeView;

public abstract class AbstractViewVaadin<P> implements CubeView {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractViewVaadin.class);

    protected final String instanceId;
    protected final ShoppingVaadinApplication app;
    protected final P presenter;
    protected Component element;

    protected AbstractViewVaadin(String instanceId, ShoppingVaadinApplication app, P presenter, Component element) {
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

    public Component getElement() {
        return this.element;
    }

    public void recreate() {
        // Override in subclasses to create fresh Vaadin components
    }

    public abstract void doUpdate();

    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception caught) {
            this.app.alertUnexpectedError(LOG, context, caught);
        }
    }

    protected <T, V extends AbstractViewVaadin<?>> BiConsumer<List<T>, List<V>> newListSlot(
            HasComponents container, Supplier<V> factory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewVaadin<?>> void syncListSlot(
            HasComponents container, List<T> items, List<V> viewList,
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
    }
}
