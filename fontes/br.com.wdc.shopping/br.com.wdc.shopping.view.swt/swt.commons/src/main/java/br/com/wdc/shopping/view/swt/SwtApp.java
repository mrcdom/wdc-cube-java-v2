package br.com.wdc.shopping.view.swt;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Minimal contract that {@link AbstractViewSwt} depends on from its host application.
 * <p>
 * Implemented by {@code ShoppingSwtApplication} (local desktop) and
 * {@code ShoppingSwtRemoteApp} (remote shell), allowing the shared SWT view
 * classes in {@code swt.commons} to be reused in both contexts.
 */
public interface SwtApp {

    /** The SWT {@link Display} driving the UI event loop. */
    Display getDisplay();

    /** The main application {@link Shell}. Used by {@code RootViewSwt} to attach itself. */
    Shell getShell();

    /**
     * A hidden off-screen {@link Composite} where views are created before
     * being slotted into the visible layout.
     */
    Composite getOffscreen();

    /**
     * Schedules {@code view} for a repaint on the next frame tick.
     * Must be callable from any thread.
     */
    void markDirty(AbstractViewSwt view);

    /**
     * Registers a {@link Composite} as the root pane in the application shell.
     * Called by {@code RootViewSwt} when it is first mounted.
     */
    void setRootPane(Composite pane);

    /**
     * Dispatches {@code action} for execution.
     * <ul>
     *   <li><b>Local mode</b>: runs on the dedicated presenter worker thread.</li>
     *   <li><b>Remote mode</b>: runs inline (WS submit is non-blocking).</li>
     * </ul>
     */
    void runAction(Runnable action);

    /**
     * Called when a view action throws an unexpected exception.
     * Default implementation does nothing (override to show error dialogs).
     */
    default void onActionError(String context, Throwable e) {
        // Default: silent — implementations may override to show a dialog
    }
}
