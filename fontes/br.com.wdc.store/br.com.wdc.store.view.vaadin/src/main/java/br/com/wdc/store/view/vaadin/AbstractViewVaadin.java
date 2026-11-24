package br.com.wdc.store.view.vaadin;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;

public abstract class AbstractViewVaadin<S extends Scope> implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewVaadin.class);

    protected final String instanceId;
    protected final StoreVaadinApplication app;
    /** The scope this view draws. */
    protected final S state;
    protected Component element;

    protected AbstractViewVaadin(String instanceId, StoreVaadinApplication app, S state, Component element) {
        this.instanceId = instanceId;
        this.app = app;
        this.state = state;
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

    protected <T, V extends AbstractViewVaadin<?>> BiConsumer<List<T>, List<V>> newListSlot(HasComponents container,
            Supplier<V> factory,
            BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            this.syncListSlot(container, items, viewList, factory, updater);
        };
    }

    private <T, V extends AbstractViewVaadin<?>> void syncListSlot(HasComponents container, List<T> items,
            List<V> viewList, Supplier<V> factory,
            BiConsumer<V, T> updater) {

        var newSize = items != null ? items.size() : 0;
        var oldSize = viewList.size();

        // Remove excess views
        if (oldSize > newSize) {
            for (int i = oldSize - 1; i >= newSize; i--) {
                container.remove(elementOf(viewList.remove(i)));
            }
        }

        // Add missing views
        while (viewList.size() < newSize) {
            var view = factory.get();
            viewList.add(view);
            container.add(elementOf(view));
        }

        // Update all
        if (items != null) {
            for (int i = 0; i < newSize; i++) {
                updater.accept(viewList.get(i), items.get(i));
            }
        }
    }

    /** The element of a view of the list (never null: the factory makes them). */
    private static Component elementOf(AbstractViewVaadin<?> view) {
        return view.getElement();
    }
}
