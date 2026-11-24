package br.com.wdc.store.view.teavm.views;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;

/**
 * Base class for all TeaVM views: draws a scope into an HTML element.
 * <p>
 * A view that holds other scopes asks for their views with {@link #viewOf} while it draws ({@link #redraw}); the views
 * it did not ask for in a drawing are the ones whose scope left, and they are released — with the views they held in
 * turn. That is how a scope that leaves the screen lets go of its elements: the presenters do not take part.
 */
public abstract class AbstractViewTeaVM<S extends Scope> {

    private static final Log LOG = Log.getLogger(AbstractViewTeaVM.class);

    protected final String instanceId;
    protected final StoreTeaVMApplication app;
    /** The scope this view draws. */
    protected final S scope;
    protected final HTMLElement element;
    protected boolean notRendered = true;
    public long dirtyTimestamp;

    /** What the scope is told to call on a change: kept to take back only this one. */
    public final Runnable updater = this::update;

    /** The views of the scopes this one holds. */
    private final Set<AbstractViewTeaVM<?>> children = new LinkedHashSet<>();

    /** The views that hold this one. */
    private final Set<AbstractViewTeaVM<?>> holders = new HashSet<>();

    /** The views asked for during the drawing under way; {@code null} outside one. */
    private Set<AbstractViewTeaVM<?>> drawn;

    private boolean released;

    protected AbstractViewTeaVM(String instanceId, StoreTeaVMApplication app, S scope, HTMLElement element) {
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

    public S scope() {
        return this.scope;
    }

    public boolean isReleased() {
        return this.released;
    }

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
    protected final AbstractViewTeaVM<?> viewOf(Scope held) {
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
     * Lets go of this view: the scope no longer tells it of changes, its element leaves the document, and so do the
     * views it held that no other view holds.
     */
    public final void release() {
        if (this.released) {
            return;
        }
        this.released = true;
        if (this.scope != null) {
            this.scope.clearUpdater(this.updater);
        }
        this.app.forgetView(this);
        if (this.element.getParentNode() != null) {
            this.element.getParentNode().removeChild(this.element);
        }
        var held = new ArrayList<>(this.children);
        this.children.clear();
        for (var child : held) {
            child.dropHolder(this);
        }
    }

    private void dropHolder(AbstractViewTeaVM<?> holder) {
        this.holders.remove(holder);
        if (this.holders.isEmpty()) {
            this.release();
        }
    }

    public HTMLElement getElement() {
        return this.element;
    }

    /**
     * Updates the DOM based on the current scope.
     * Called through {@link #redraw} when the view is marked as dirty.
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
