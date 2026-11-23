package br.com.wdc.shopping.view.remote.shell.cn1.views;

import com.codename1.ui.Container;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/** App root (classId {@value #CLASS_ID}) — mounts the content view (login or home). */
public class RootCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "f2d345c4a610";

    private Slot slot;

    public RootCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        slot = new Slot();
        return slot;
    }

    /**
     * Only <b>mounts</b> the content screen (login/home). The child's data update is dispatched
     * by the bridge (the flush dispatches doUpdate per received ViewState), so there is no
     * manual propagation here.

     */
    @Override
    public void doUpdate() {
        slot.mount(childElement(Json.str(state(), "contentViewId")));
    }
}
