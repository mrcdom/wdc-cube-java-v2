package br.com.wdc.shopping.view.remote.shell.codenameone.bridge;

import com.codename1.ui.Container;

/**
 * Base de uma <b>item-view</b> de lista (sem vsid próprio) — usada pelo {@code syncList} para
 * representar cada item de uma coleção (produto, item de carrinho, etc.). Mesmo padrão não-reativo:
 * constrói os widgets uma vez, {@link #setState(Object)} muta-os a partir do dado do item.
 */
public abstract class AbstractItemCn1<T> {

    private Container element;
    private boolean built;
    protected T data;

    public final Container getElement() {
        if (!built) {
            built = true;
            element = build();
        }
        return element;
    }

    protected abstract Container build();

    public final void setState(T data) {
        this.data = data;
        getElement();
        doUpdate();
    }

    protected abstract void doUpdate();
}
