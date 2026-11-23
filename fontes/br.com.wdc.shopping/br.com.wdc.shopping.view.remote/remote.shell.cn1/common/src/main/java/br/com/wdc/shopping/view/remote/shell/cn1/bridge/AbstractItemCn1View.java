package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

import com.codename1.ui.Container;

/**
 * Base of a list <b>item view</b> (no vsid of its own) — used by {@code syncList} to represent
 * each item of a collection (product, cart item, etc.). Same non-reactive pattern: builds the
 * widgets once, {@link #setState(Object)} mutates them from the item's data.
 */
public abstract class AbstractItemCn1View<T> {

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

    /** Re-applies {@link #doUpdate()} from the last data (no-op if not built yet/no data).
 */
    public final void refresh() {
        if (built && data != null) {
            doUpdate();
        }
    }

    protected abstract void doUpdate();
}
