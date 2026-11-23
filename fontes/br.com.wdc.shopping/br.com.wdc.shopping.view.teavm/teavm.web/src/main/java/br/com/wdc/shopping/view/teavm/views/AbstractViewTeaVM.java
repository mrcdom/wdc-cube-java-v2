package br.com.wdc.shopping.view.teavm.views;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.commons.log.Log;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;

/**
 * Classe base para todas as views TeaVM.
 * Equivalente a AbstractViewVaadin/AbstractViewSwt mas operando sobre HTMLElement via TeaVM.
 */
public abstract class AbstractViewTeaVM<S extends Scope> implements CubeView {

    private static final Log LOG = Log.getLogger(AbstractViewTeaVM.class);

    protected final String instanceId;
    protected final ShoppingTeaVMApplication app;
    /** The scope this view draws. */
    protected final S state;
    protected final HTMLElement element;
    protected boolean notRendered = true;
    public long dirtyTimestamp;

    protected AbstractViewTeaVM(String instanceId, ShoppingTeaVMApplication app, S state, HTMLElement element) {
        this.instanceId = instanceId;
        this.app = app;
        this.state = state;
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
        if (this.element.getParentNode() != null) {
            this.element.getParentNode().removeChild(this.element);
        }
    }

    public HTMLElement getElement() {
        return this.element;
    }

    /**
     * Atualiza o DOM com base no estado atual do escopo.
     * Chamado pelo render loop quando a view está marcada como dirty.
     */
    public abstract void doUpdate();

    /**
     * Executa uma ação do presenter em um Thread TeaVM (contexto de coroutine),
     * permitindo que chamadas {@code @Async} (como HTTP requests) possam suspender.
     * Erros são propagados para a aplicação via {@code alertUnexpectedError}.
     */
    protected void safeAction(String context, Runnable action) {
        new Thread(() -> {
            try {
                action.run();
            } catch (Exception e) {
                app.alertUnexpectedError(LOG, context, e);
            }
        }).start();
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
     * Cria um slot de lista sincronizado com o DOM, padrão equivalente ao AbstractViewVaadin.newListSlot.
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
