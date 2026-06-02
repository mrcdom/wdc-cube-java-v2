package br.com.wdc.framework.cube.remote.bridge.teavm;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * GC de ViewScopes baseado em autoridade do servidor.
 *
 * O servidor informa periodicamente (via resposta ao ping) quais views estão ativas.
 * O cliente remove da viewMap qualquer view que:
 *   1. NÃO conste na lista do servidor
 *   2. NÃO esteja montada (bindView ativo)
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
