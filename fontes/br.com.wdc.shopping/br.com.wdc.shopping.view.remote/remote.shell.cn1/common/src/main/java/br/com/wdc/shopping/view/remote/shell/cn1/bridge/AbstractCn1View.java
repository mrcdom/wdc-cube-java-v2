package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.codename1.ui.Container;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;

/**
 * Base de uma view do shell (uma por vsid), no padrão <b>não-reativo</b> (estilo
 * {@code AbstractViewVaadin}): os widgets são construídos <b>uma vez</b> em {@link #build()} e o
 * {@link #doUpdate()} <b>muta</b> os widgets a partir do estado — nunca reconstrói a árvore.
 *
 * <p>
 * Sendo um shell <b>remoto</b>, o estado é lido como JSON por nome ({@link #state()} = mapa vindo do
 * bridge) e os eventos são submetidos pelo bridge ({@link #submit(int, Map)}). Views-pai montam
 * filhas por vsid via {@code Slot} (passando {@link #childElement(String)}).
 * </p>
 */
public abstract class AbstractCn1View {

    protected final String vsid;
    protected final BridgeSession session;
    protected final ShoppingCn1RemoteApp app;

    private Container element;
    private boolean built;

    protected AbstractCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        this.vsid = vsid;
        this.session = session;
        this.app = app;
    }

    public final String vsid() {
        return vsid;
    }

    /** Component raiz — construído uma vez (lazy) e já sincronizado. */
    public final Container getElement() {
        if (!built) {
            built = true;
            element = build();
            doUpdate();
        }
        return element;
    }

    /** Element já construído, ou {@code null} se ainda não — não dispara o build (usado no descarte). */
    public final Container peekElement() {
        return element;
    }

    /** Cria a árvore de widgets uma única vez. */
    protected abstract Container build();

    /** Sincroniza os widgets a partir do estado atual (muta, não reconstrói). */
    public abstract void doUpdate();

    /** Agenda um re-render desta view (primitivo único; igual ao {@code markDirty} do shell SWT). */
    public void update() {
        app.markDirty(this);
    }

    protected Map<String, Object> state() {
        Map<String, Object> s = session.state(vsid);
        return s != null ? s : Collections.emptyMap();
    }

    protected void submit(int eventCode, Map<String, Object> form) {
        session.submit(vsid, eventCode, form);
    }

    protected void submit(int eventCode) {
        session.submit(vsid, eventCode, new HashMap<>());
    }

    /** View filha (criada sob demanda via o app), ou {@code null}. */
    protected AbstractCn1View childView(String childVsid) {
        return app.viewFor(childVsid);
    }

    /** Element da view filha de {@code childVsid} (criado sob demanda), ou {@code null}. Monte num {@code Slot}. */
    protected Container childElement(String childVsid) {
        AbstractCn1View v = childView(childVsid);
        return v != null ? v.getElement() : null;
    }

    /**
     * Sincroniza uma lista de itens com uma lista de item-views, <b>reusando</b> as item-views
     * (cresce/encolhe/atualiza) — equivalente ao {@code syncListSlot} do Vaadin.
     */
    protected <T, V extends AbstractItemCn1View<T>> void syncList(Container container, List<T> items,
            List<V> viewList, Supplier<V> factory) {
        int n = items != null ? items.size() : 0;
        while (viewList.size() < n) {
            V v = factory.get();
            viewList.add(v);
            container.add(v.getElement());
        }
        for (int i = viewList.size() - 1; i >= n; i--) {
            V v = viewList.remove(i);
            container.removeComponent(v.getElement());
        }
        for (int i = 0; i < n; i++) {
            viewList.get(i).setState(items.get(i));
        }
    }
}
