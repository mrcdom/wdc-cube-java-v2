package br.com.wdc.shopping.view.teavm.vdom;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;

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
 * public class MyView extends AbstractVDomView<MyPresenter> {
 *
 *     public MyView(MyPresenter presenter) {
 *         super("my-view", (ShoppingTeaVMApplication) presenter.app, presenter);
 *     }
 *
 *     @Override
 *     protected VNode render() {
 *         return div("container").children(
 *             h5("title").text(state.title),
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
public abstract class AbstractVDomView<P> extends AbstractViewTeaVM<P> {

    private VNode prevTree;

    protected AbstractVDomView(String instanceId, ShoppingTeaVMApplication app, P presenter) {
        super(instanceId, app, presenter, HTMLDocument.current().createElement("div"));
    }

    protected AbstractVDomView(String instanceId, ShoppingTeaVMApplication app, P presenter, HTMLElement element) {
        super(instanceId, app, presenter, element);
    }

    /**
     * Descreve a árvore virtual da view com base no estado atual.
     * Chamado a cada ciclo de render — o VDom aplica apenas as diferenças no DOM real.
     */
    protected abstract VNode render();

    @Override
    public void doUpdate() {
        var nextTree = render();
        prevTree = VDom.patch(this.element, prevTree, nextTree);
    }
}
