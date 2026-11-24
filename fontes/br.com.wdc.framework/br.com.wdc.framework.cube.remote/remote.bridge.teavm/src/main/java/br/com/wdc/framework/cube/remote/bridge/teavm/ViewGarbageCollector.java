package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ViewScope GC driven by server authority.
 *
 * The server periodically reports (via the ping response) which views are active.
 * The client removes from the viewMap any view that:
 *   1. is NOT in the server's list
 *   2. is NOT mounted (no active bindView)
 */
public class ViewGarbageCollector {

    private final ViewStateCoordinator app;
    private final Set<String> mountedViews = new HashSet<>();

    public ViewGarbageCollector(ViewStateCoordinator app) {
        this.app = app;
    }

    public void mount(String vsid) {
        mountedViews.add(vsid);
    }

    public void unmount(String vsid) {
        mountedViews.remove(vsid);
    }

    public void release(List<String> releasedViews) {
        for (String vsid : releasedViews) {
            if (!mountedViews.contains(vsid)) {
                app.viewMap.remove(vsid);
            }
        }
    }

    public void sweep(List<String> activeViews) {
        Set<String> serverActive = new HashSet<>(activeViews);
        var keysToRemove = new java.util.ArrayList<String>();

        for (String vsid : app.viewMap.keySet()) {
            if (!serverActive.contains(vsid) && !mountedViews.contains(vsid)) {
                keysToRemove.add(vsid);
            }
        }

        for (String vsid : keysToRemove) {
            app.viewMap.remove(vsid);
        }
    }
}
