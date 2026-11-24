package br.com.wdc.store.view.remote.shell.cn1.widgets;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

/**
 * <b>Single-child</b> container: holds one {@link Component} and only touches the tree on transition.
 *
 * <p>
 * Detects change by <b>reference comparison</b>: since the app always resolves a vsid to the same
 * view instance (and {@code view.getElement()} always to the same element), comparing the child's
 * reference is enough to know whether it changed. That is why the widget is <b>generic</b> — it knows
 * neither vsid nor the app; the caller resolves the {@code Component} (a {@code view.getElement()} or a
 * local component) and passes it.
 * </p>
 */
public final class Slot extends Container {

    private Component current;

    public Slot() {
        super(new BorderLayout());
    }

    /** Ensures {@code child} is the only child ({@code null} clears). Re-mounts only when the reference changes. */
    public void mount(Component child) {
        if (child == current) {
            return;
        }
        current = child;
        removeAll();
        if (child != null) {
            // detaches from the previous parent (piece reuse in a rearrangement): CN1's add() throws if it already has a parent

            if (child.getParent() != null) {
                child.getParent().removeComponent(child);
            }
            add(BorderLayout.CENTER, child);
        }
        revalidate();
    }
}
