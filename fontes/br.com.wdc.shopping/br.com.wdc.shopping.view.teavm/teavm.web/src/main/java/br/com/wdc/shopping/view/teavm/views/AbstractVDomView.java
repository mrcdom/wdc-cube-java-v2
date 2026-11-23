package br.com.wdc.shopping.view.teavm.views;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VDom;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Base para views TeaVM com Virtual DOM.
 * <p>
 * Substitui o padrão imperativo (criar DOM + atualizar manualmente) por um
 * modelo declarativo: a view implementa {@link #render()} que retorna uma árvore
 * de {@link VNode}. O VDom cuida do diffing e patching automático.
 * <p>
 * Exemplo:
 * <pre>{@code
 * import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;
 *
 * public class MyView extends AbstractVDomView<MyScope> {
 *
 *     public MyView(ShoppingTeaVMApplication app, MyScope state) {
 *         super("my-view", app, state);
 *     }
 *
 *     @Override
 *     protected VNode render() {
 *         return div("container").children(
 *             h5("title").text(state.title.get()),
 *             div("content").children(
 *                 items.stream().map(item ->
 *                     div("item").key(item.id).text(item.name)
 *                 ).toList()
 *             ),
 *             button("btn btn-primary").text("OK").on("click", e -> safeAction("OK", this::onOk))
 *         );
 *     }
 * }
 * }</pre>
 */
public abstract class AbstractVDomView<S extends Scope> extends AbstractViewTeaVM<S> {

    private VNode prevTree;

    // Listener memoization: stable references across renders
    private Map<String, EventListener<? extends Event>> listenerCache = new HashMap<>();
    private Set<String> usedKeys = new HashSet<>();

    protected AbstractVDomView(String instanceId, ShoppingTeaVMApplication app, S state) {
        super(instanceId, app, state, HTMLDocument.current().createElement("div"));
    }

    protected AbstractVDomView(String instanceId, ShoppingTeaVMApplication app, S state, HTMLElement element) {
        super(instanceId, app, state, element);
    }

    /**
     * Descreve a árvore virtual da view com base no estado atual.
     * Chamado a cada ciclo de render — o VDom aplica apenas as diferenças no DOM real.
     */
    protected abstract VNode render();

    @Override
    public void doUpdate() {
        usedKeys.clear();
        var nextTree = render();
        listenerCache.keySet().retainAll(usedKeys);
        prevTree = VDom.patch(this.element, prevTree, nextTree);
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
}
