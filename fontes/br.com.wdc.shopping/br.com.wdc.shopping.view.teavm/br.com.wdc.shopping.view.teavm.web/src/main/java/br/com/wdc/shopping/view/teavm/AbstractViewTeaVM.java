package br.com.wdc.shopping.view.teavm;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.view.teavm.interop.Console;

/**
 * Classe base para todas as views TeaVM.
 * Equivalente a AbstractViewGluon mas operando sobre HTMLElement via TeaVM.
 */
public abstract class AbstractViewTeaVM<P> implements CubeView {

    protected final String instanceId;
    protected final ShoppingTeaVMApplication app;
    protected final P presenter;
    protected final HTMLElement element;
    protected boolean notRendered = true;
    long dirtyTimestamp;

    protected AbstractViewTeaVM(String instanceId, ShoppingTeaVMApplication app, P presenter, HTMLElement element) {
        this.instanceId = instanceId;
        this.app = app;
        this.presenter = presenter;
        this.element = element;
    }

    @Override
    public String instanceId() {
        return this.instanceId;
    }

    @Override
    public void update() {
        this.app.markDirty(this);
    }

    @Override
    public void release() {
        this.element.getOwnerDocument().getBody().removeChild(this.element);
    }

    public HTMLElement getElement() {
        return this.element;
    }

    /**
     * Atualiza o DOM com base no estado atual do presenter.
     * Chamado pelo render loop quando a view está marcada como dirty.
     */
    public abstract void doUpdate();

    /**
     * Executa uma ação do presenter com tratamento de erro.
     */
    protected void safeAction(String context, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            Console.error("[" + context + "] Error: " + e.getMessage());
        }
    }

    /**
     * Mostra/esconde elemento usando a classe Bootstrap d-none.
     */
    protected void setVisible(HTMLElement el, boolean visible) {
        if (el != null) {
            if (visible) {
                el.getClassList().remove("d-none");
            } else {
                el.getClassList().add("d-none");
            }
        }
    }

    /**
     * Cria um slot de lista sincronizado com o DOM, padrão equivalente ao GluonDom.newListSlot.
     * Cresce a lista de views conforme necessário, esconde as excedentes e atualiza as visíveis.
     */
    protected <T, V extends AbstractViewTeaVM<?>> BiConsumer<List<T>, List<V>> newListSlot(
            HTMLElement container, Supplier<V> viewFactory, BiConsumer<V, T> updater) {
        return (items, viewList) -> {
            var itemCount = items != null ? items.size() : 0;

            // Grow viewList if needed
            while (viewList.size() < itemCount) {
                var newView = viewFactory.get();
                viewList.add(newView);
                container.appendChild(newView.getElement());
            }

            // Hide extras
            for (int i = itemCount; i < viewList.size(); i++) {
                setVisible(viewList.get(i).getElement(), false);
            }

            // Show and update visible items
            for (int i = 0; i < itemCount; i++) {
                setVisible(viewList.get(i).getElement(), true);
                updater.accept(viewList.get(i), items.get(i));
            }
        };
    }

}
