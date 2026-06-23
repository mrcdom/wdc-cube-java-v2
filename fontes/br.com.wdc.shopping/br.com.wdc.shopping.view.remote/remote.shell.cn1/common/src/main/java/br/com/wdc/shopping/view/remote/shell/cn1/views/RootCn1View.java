package br.com.wdc.shopping.view.remote.shell.cn1.views;

import com.codename1.ui.Container;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

/** Raiz do app (classId {@value #CLASS_ID}) — monta a view de conteúdo (login ou home). */
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
     * Apenas <b>monta</b> a tela de conteúdo (login/home). A atualização de dados da filha é
     * despachada pela bridge (o flush despacha doUpdate por ViewState recebido), então não há
     * propagação manual aqui.
     */
    @Override
    public void doUpdate() {
        slot.mount(childElement(Json.str(state(), "contentViewId")));
    }
}
