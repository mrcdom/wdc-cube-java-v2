package br.com.wdc.store.view.vaadin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.Scope;

/**
 * A Vaadin view: draws a scope into a component.
 * <p>
 * A view that holds other scopes asks for their views with {@link #viewOf} while it draws ({@link #redraw}); the views
 * it did not ask for in a drawing are the ones whose scope left, and they are released — with the views they held in
 * turn. That is how a scope that leaves the screen lets go of its components: the presenters do not take part.
 */
public abstract class AbstractViewVaadin<S extends Scope> {

    private static final Log LOG = Log.getLogger(AbstractViewVaadin.class);

    protected final String instanceId;
    protected final StoreVaadinApplication app;
    /** The scope this view draws. */
    protected final S scope;
    protected Component element;

    /** What the scope is told to call on a change: kept to take back only this one. */
    final Runnable updater = this::update;

    /** The views of the scopes this one holds. */
    private final Set<AbstractViewVaadin<?>> children = new LinkedHashSet<>();

    /** The views that hold this one. */
    private final Set<AbstractViewVaadin<?>> holders = new HashSet<>();

    /** The views asked for during the drawing under way; {@code null} outside one. */
    private Set<AbstractViewVaadin<?>> drawn;

    private boolean released;

    protected AbstractViewVaadin(String instanceId, StoreVaadinApplication app, S scope, Component element) {
        this.instanceId = instanceId;
        this.app = app;
        this.scope = scope;
        this.element = element;
    }

    public String instanceId() {
        return this.instanceId;
    }

    /** The scope changed: draw it again. */
    public void update() {
        this.app.markDirty(this);
    }

    public boolean isReleased() {
        return this.released;
    }

    public Component getElement() {
        return this.element;
    }

    public void recreate() {
        // Override in subclasses to create fresh Vaadin components
    }

    /** Pushes the scope's state into the components. Called through {@link #redraw}. */
    public abstract void doUpdate();

    /** Draws the scope; the held views not asked for while drawing are released. */
    public final void redraw() {
        if (this.released) {
            return;
        }
        this.drawn = new HashSet<>();
        try {
            this.doUpdate();
        } finally {
            var kept = this.drawn;
            this.drawn = null;
            for (var child : new ArrayList<>(this.children)) {
                if (!kept.contains(child)) {
                    this.children.remove(child);
                    child.dropHolder(this);
                }
            }
        }
    }

    /** The view of a scope this one holds (a presenter's below, a segment, a row); {@code null} for no scope. */
    protected final AbstractViewVaadin<?> viewOf(Scope held) {
        var child = this.app.viewOf(held);
        if (child != null) {
            child.holders.add(this);
            this.children.add(child);
            if (this.drawn != null) {
                this.drawn.add(child);
            }
        }
        return child;
    }

    /**
     * Lets go of this view: the scope no longer tells it of changes, its component leaves the screen, and so do the
     * views it held that no other view holds.
     */
    public final void release() {
        if (this.released) {
            return;
        }
        this.released = true;
        this.scope.clearUpdater(this.updater);
        this.app.forgetView(this);
        try {
            this.onRelease();
        } catch (Exception caught) {
            LOG.error("Releasing " + this.instanceId, caught);
        }
        var held = new ArrayList<>(this.children);
        this.children.clear();
        for (var child : held) {
            child.dropHolder(this);
        }
    }

    /** What a view undoes when released; by default, its component leaves its parent. */
    protected void onRelease() {
        if (this.element != null) {
            this.element.getElement().removeFromParent();
        }
    }

    private void dropHolder(AbstractViewVaadin<?> holder) {
        this.holders.remove(holder);
        if (this.holders.isEmpty()) {
            this.release();
        }
    }

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
