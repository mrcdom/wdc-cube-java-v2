package br.com.wdc.shopping.view.remote.shell.teavm.bridge;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.vdom.VDom;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.interop.JsRunnable;

/**
 * Base class for all remote views using Virtual DOM rendering.
 * <p>
 * Unlike the local TeaVM views that read state directly from presenters,
 * remote views receive state from the server via {@link ViewScope} (JSON map).
 * The rendering uses the same VDom/Swc infrastructure.
 */
public abstract class AbstractRemoteView {

    private final String vsid;
    private final HTMLElement element;
    private VNode prevTree;
    private boolean updateScheduled = false;

    protected AbstractRemoteView(String vsid) {
        this.vsid = vsid;
        this.element = HTMLDocument.current().createElement("div");
    }

    protected AbstractRemoteView(String vsid, String tag) {
        this.vsid = vsid;
        this.element = HTMLDocument.current().createElement(tag);
    }

    public String getVsid() {
        return vsid;
    }

    public HTMLElement getElement() {
        return element;
    }

    /**
     * Guarantees that {@link #doUpdate()} will run on the next animation frame.
     * Multiple calls coalesce into a single doUpdate execution.
     * The scheduling flag resets before doUpdate runs, so exceptions
     * do not prevent future retries.
     */
    public void forceUpdate() {
        if (!updateScheduled) {
            updateScheduled = true;
            scheduleAnimationFrame((JsRunnable) () -> {
                updateScheduled = false;
                doUpdate();
            });
        }
    }

    /**
     * Render cycle: diffs and patches the DOM.
     */
    public void doUpdate() {
        var nextTree = render();
        prevTree = VDom.patch(element, prevTree, nextTree);
    }

    /**
     * Subclasses implement this to describe the view tree based on current state.
     */
    protected abstract VNode render();

    // -- Helpers --

    protected ViewScope state() {
        ViewScope scope = ViewStateCoordinator.INSTANCE.getViewScope(vsid);
        if (scope == null) {
            scope = new ViewScope(vsid);
            scope.setForceUpdate(this::forceUpdate);
        }
        return scope;
    }

    protected void submit(int eventId) {
        ViewStateCoordinator.INSTANCE.submit(vsid, eventId);
    }

    protected void setFormField(String fieldName, Object value) {
        ViewStateCoordinator.INSTANCE.setFormField(vsid, fieldName, value);
    }

    protected HTMLElement getChildViewElement(String childVsid) {
        return ViewStateCoordinator.INSTANCE.getViewElement(childVsid);
    }

    @JSBody(params = {"callback"}, script = "requestAnimationFrame(callback);")
    private static native void scheduleAnimationFrame(JsRunnable callback);
}
