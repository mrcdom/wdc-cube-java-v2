package br.com.wdc.shopping.view.robovm;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.CubeViewSlot;

/**
 * Thread-safe view slot implementation for RoboVM/iOS.
 * Manages a reference to a CubeView that can be set from any thread.
 */
public class RoboVMViewSlot implements CubeViewSlot {

    private volatile CubeView view;
    private CubeView flushedView;

    @Override
    public void setView(CubeView view) {
        this.view = view;
    }

    public CubeView getView() {
        return this.flushedView;
    }

    /**
     * Call from main thread to commit the pending view reference.
     *
     * @return true if the view changed
     */
    public boolean flush() {
        var newView = this.view;
        if (this.flushedView != newView) {
            this.flushedView = newView;
            return true;
        }
        return false;
    }
}
