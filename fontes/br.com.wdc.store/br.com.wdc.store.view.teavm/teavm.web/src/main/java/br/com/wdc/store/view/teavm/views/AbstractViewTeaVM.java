package br.com.wdc.store.view.teavm.views;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;

/**
 * Base class for all TeaVM views.
 * Equivalent to AbstractViewVaadin/AbstractViewSwt but operating on HTMLElement through TeaVM.
 */
public abstract class AbstractViewTeaVM<S extends Scope> implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewTeaVM.class);

    protected final String instanceId;
    protected final StoreTeaVMApplication app;
    /** The scope this view draws. */
    protected final S state;
    protected final HTMLElement element;
    protected boolean notRendered = true;
    public long dirtyTimestamp;

    protected AbstractViewTeaVM(String instanceId, StoreTeaVMApplication app, S state, HTMLElement element) {
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
        if (this.element.getParentNode() != null) {
            this.element.getParentNode().removeChild(this.element);
        }
    }

    public HTMLElement getElement() {
        return this.element;
    }

    /**
     * Updates the DOM based on the current state of the scope.
     * Called by the render loop when the view is marked as dirty.
     */
    public abstract void doUpdate();

    /**
     * Runs a presenter action on a TeaVM Thread (coroutine context),
     * allowing {@code @Async} calls (such as HTTP requests) to suspend.
     * Errors are propagated to the application through {@code alertUnexpectedError}.
     */
    protected void safeAction(String context, Runnable action) {
        new Thread(() -> {
            try {
                action.run();
            } catch (Exception e) {
                app.alertUnexpectedError(LOG, context, e);
            }
        }).start();
    }

    /**
     * Shows/hides an element using the Bootstrap d-none class.
     */
    protected void setVisible(HTMLElement el, boolean visible) {
        if (el != null) {
            if (visible) {
                el.getClassList().remove("d-none");
            } else {
                el.getClassList().add("d-none");
            }
        }
    }

    /**
     * Creates a list slot synchronized with the DOM, the same pattern as AbstractViewVaadin.newListSlot.
     * Grows the view list as needed, hides the extra ones and updates the visible ones.
     */
    protected <T, V extends AbstractViewTeaVM<?>> BiConsumer<List<T>, List<V>> newListSlot(
            HTMLElement container, Supplier<V> viewFactory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            var itemCount = items != null ? items.size() : 0;

            // Grow viewList if needed
            while (viewList.size() < itemCount) {
                var newView = viewFactory.get();
                viewList.add(newView);
                container.appendChild(newView.getElement());
            }

            // Hide extras
            for (int i = itemCount; i < viewList.size(); i++) {
                setVisible(viewList.get(i).getElement(), false);
            }

            // Show and update visible items
            for (int i = 0; i < itemCount; i++) {
                setVisible(viewList.get(i).getElement(), true);
                updater.accept(viewList.get(i), items.get(i));
            }
        };
    }

}
