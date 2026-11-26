package br.com.wdc.store.view.swt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.widgets.Composite;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.Scope;

/**
 * An SWT view: draws a scope into a composite.
 * <p>
 * A view that holds other scopes asks for their views with {@link #viewOf} (or takes them with {@link #hold}) while it
 * draws ({@link #performUpdate}); a held view it did not ask for in a drawing is let go, and once no view holds it,
 * the application decides what that means ({@link SwtApp#viewLeft}): the local one releases it, the remote shell waits
 * for the host to say so. A released view disposes its composite and lets go of the views it held in turn.
 */
public abstract class AbstractViewSwt {

    private static final Log LOG = Log.getLogger(AbstractViewSwt.class);

    /** Numbers every view: a scope type can have many views at once (the cart's rows). */
    private static final AtomicLong SEQUENCE = new AtomicLong();

    protected final String instanceId;
    protected final SwtApp app;
    protected final Composite element;

    long dirtyTimestamp;
    private boolean needsInitialLayout = true;

    /** What the scope is told to call on a change: kept to take back only this one. */
    public final Runnable updater = this::update;

    /** The views of the scopes this one holds. */
    private final Set<AbstractViewSwt> children = new LinkedHashSet<>();

    /** The views that hold this one. */
    private final Set<AbstractViewSwt> holders = new HashSet<>();

    /** The views asked for during the drawing under way; {@code null} outside one. */
    private Set<AbstractViewSwt> drawn;

    private boolean released;

    /** @param name the view's kind (e.g. {@code "cart-item"}); the instance id adds a number unique to this view */
    protected AbstractViewSwt(String name, SwtApp app) {
        this(name, app, new Composite(app.getOffscreen(), SWT.NONE));
    }

    protected AbstractViewSwt(String name, SwtApp app, Composite element) {
        this.instanceId = name + ":" + SEQUENCE.incrementAndGet();
        this.app = app;
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

    /** The view of a scope this one holds (a presenter's below, a segment, a row); {@code null} for none. */
    protected final AbstractViewSwt viewOf(Scope held) {
        return this.hold(this.app.viewOf(held));
    }

    /** Takes a view this one holds; {@code null} for none. */
    protected final AbstractViewSwt hold(AbstractViewSwt child) {
        if (child != null && !child.released) {
            child.holders.add(this);
            this.children.add(child);
            if (this.drawn != null) {
                this.drawn.add(child);
            }
        }
        return child;
    }

    /**
     * Lets go of this view: its composite is disposed (on the UI thread), and the views it held that no other view
     * holds are let go too.
     */
    public final void release() {
        if (this.released) {
            return;
        }
        this.released = true;
        this.app.forgetView(this);
        var display = this.app.getDisplay();
        Runnable dispose = () -> {
            if (!this.element.isDisposed()) {
                this.element.dispose();
            }
        };
        if (display.getThread() == Thread.currentThread()) {
            dispose.run();
        } else if (!display.isDisposed()) {
            display.asyncExec(dispose);
        }
        var held = new ArrayList<>(this.children);
        this.children.clear();
        for (var child : held) {
            child.dropHolder(this);
        }
    }

    private void dropHolder(AbstractViewSwt holder) {
        this.holders.remove(holder);
        if (this.holders.isEmpty()) {
            this.app.viewLeft(this);
        }
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
        if (this.released || this.element.isDisposed()) {
            return;
        }
        this.drawn = new HashSet<>();
        try {
            doUpdate();
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
        if (this.needsInitialLayout) {
            this.needsInitialLayout = false;
            this.element.layout(true, true);
        }
    }

    /**
     * For a view whose size changed after its owner laid it out (a row drawn after the cart placed it): lays the
     * ancestors out again and resizes the content of the scrolled page around it, which keeps its size otherwise.
     */
    protected void relayoutAncestors() {
        this.element.requestLayout();
        for (var parent = this.element.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof ScrolledComposite scrolled && scrolled.getContent() != null) {
                var width = scrolled.getClientArea().width;
                var content = scrolled.getContent();
                content.setSize(content.computeSize(width > 0 ? width : SWT.DEFAULT, SWT.DEFAULT, true));
                return;
            }
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
