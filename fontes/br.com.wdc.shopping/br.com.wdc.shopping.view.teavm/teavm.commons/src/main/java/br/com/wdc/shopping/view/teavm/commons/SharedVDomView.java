package br.com.wdc.shopping.view.teavm.commons;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.view.teavm.commons.interop.JsRunnable;

/**
 * Base for shared views using Virtual DOM rendering.
 * <p>
 * This class handles the render lifecycle (VDom diff/patch, listener memoization,
 * requestAnimationFrame scheduling) without coupling to any specific application
 * framework (no presenter, no remote bridge). Subclasses implement {@link #render()}.
 * <p>
 * Each consuming project (teavm.web, remote.shell.teavm) provides the state supplier
 * and action listeners externally.
 */
public abstract class SharedVDomView {

    private final HTMLElement element;
    private VNode prevTree;
    private boolean updateScheduled = false;

    // Listener memoization: stable references across renders
    private final Map<String, EventListener<? extends Event>> listenerCache = new HashMap<>();
    private final Set<String> usedKeys = new HashSet<>();

    protected SharedVDomView() {
        this.element = HTMLDocument.current().createElement("div");
    }

    protected SharedVDomView(String tag) {
        this.element = HTMLDocument.current().createElement(tag);
    }

    public HTMLElement getElement() {
        return element;
    }

    /**
     * Subclasses implement this to describe the view tree based on current state.
     */
    public abstract VNode render();

    /**
     * Schedules a re-render on the next animation frame.
     * Multiple calls coalesce into a single render.
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
     * Also performs listener cache cleanup (removes entries not used this cycle).
     */
    public void doUpdate() {
        usedKeys.clear();
        var nextTree = render();
        listenerCache.keySet().retainAll(usedKeys);
        prevTree = VDom.patch(element, prevTree, nextTree);
    }

    /**
     * Renders the view tree and performs listener cache cleanup, but does NOT patch DOM.
     * Use this from adapter's render() method when the adapter manages its own DOM patching.
     */
    public VNode renderTree() {
        usedKeys.clear();
        var result = render();
        listenerCache.keySet().retainAll(usedKeys);
        return result;
    }

    /**
     * Returns a stable event listener for the given key.
     * If the key was seen before, returns the cached instance (same reference = no DOM re-registration).
     * If the key is new, stores and returns the provided listener.
     * Listeners not referenced during a render cycle are automatically removed.
     */
    @SuppressWarnings("unchecked")
    protected <T extends Event> EventListener<T> useCallback(String key, EventListener<T> listener) {
        usedKeys.add(key);
        return (EventListener<T>) listenerCache.computeIfAbsent(key, k -> listener);
    }

    @JSBody(params = {"callback"}, script = "requestAnimationFrame(callback);")
    private static native void scheduleAnimationFrame(JsRunnable callback);
}
