package br.com.wdc.shopping.view.remote.shell.cn1.views;

import com.codename1.ui.Container;
import com.codename1.ui.layouts.BorderLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;

/** Raiz do app (classId {@value #CLASS_ID}) — monta a view de conteúdo (login ou home). */
public class RootCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "f2d345c4a610";

    private Container root;
    private String mountedVsid = "";

    public RootCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        root = new Container(new BorderLayout());
        return root;
    }

    @Override
    public void doUpdate() {
        String contentVsid = Json.str(state(), "contentViewId");
        if (!contentVsid.isEmpty() && !contentVsid.equals(mountedVsid)) {
            mountedVsid = contentVsid;
            Container el = childElement(contentVsid);
            root.removeAll();
            if (el != null) {
                root.add(BorderLayout.CENTER, el);
            }
        }
        AbstractCn1View content = childView(contentVsid);
        if (content != null) {
            content.doUpdate();
        }
    }
}
