package br.com.wdc.shopping.view.remote.shell.cn1.bridge;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.codename1.ui.Container;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;

/**
 * Base of a shell view (one per vsid), in the <b>non-reactive</b> pattern ({@code AbstractViewVaadin}
 * style): the widgets are built <b>once</b> in {@link #build()} and {@link #doUpdate()} <b>mutates</b>
 * the widgets from the state — it never rebuilds the tree.
 *
 * <p>
 * Being a <b>remote</b> shell, the state is read as JSON by name ({@link #state()} = map coming from
 * the bridge) and events are submitted through the bridge ({@link #submit(int, Map)}). Parent views
 * mount children by vsid via {@code Slot} (passing {@link #childElement(String)}).
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

    /** Root component — built once (lazily) and already synced. */
    public final Container getElement() {
        if (!built) {
            built = true;
            element = build();
            doUpdate();
        }
        return element;
    }

    /** Element already built, or {@code null} if not yet — does not trigger the build (used on disposal). */
    public final Container peekElement() {
        return element;
    }

    /** Creates the widget tree a single time. */
    protected abstract Container build();

    /** Syncs the widgets from the current state (mutates, does not rebuild). */
    public abstract void doUpdate();

    /** Schedules a re-render of this view (single primitive; same as the SWT shell's {@code markDirty}). */
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

    /** Child view (created on demand via the app), or {@code null}. */
    protected AbstractCn1View childView(String childVsid) {
        return app.viewFor(childVsid);
    }

    /** Element of the {@code childVsid} child view (created on demand), or {@code null}. Mount it in a {@code Slot}. */
    protected Container childElement(String childVsid) {
        AbstractCn1View v = childView(childVsid);
        return v != null ? v.getElement() : null;
    }

    /**
     * Syncs a list of items with a list of item views, <b>reusing</b> the item views
     * (grows/shrinks/updates) — equivalent to Vaadin's {@code syncListSlot}.

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
