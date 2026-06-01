package br.com.wdc.framework.cube.remote.bridge.teavm;

import org.teavm.jso.dom.html.HTMLElement;

/**
 * Interface for remote views managed by the ViewStateCoordinator.
 * Implementations provide DOM rendering; the bridge manages state and lifecycle.
 */
public interface RemoteView {

    String getVsid();

    HTMLElement getElement();

    void forceUpdate();
}
