package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Tela principal (classId {@value #CLASS_ID}): cabeçalho (nick + carrinho + sair) e uma área de
 * conteúdo que mostra o painel de produtos por padrão, ou uma tela aninhada (produto/carrinho/
 * recibo) quando o servidor define {@code contentViewId}.
 */
public class HomeCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "473dbdd7a36a";
    private static final int EVT_LOGOUT = 1;
    private static final int EVT_OPEN_CART = 2;

    private Label nick;
    private Button cartBtn;
    private Container contentPane;
    private String mountedVsid = "";

    public HomeCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container c = new Container(new BorderLayout());

        Container header = new Container(BoxLayout.x());
        nick = new Label("");
        cartBtn = new Button("Carrinho");
        cartBtn.addActionListener(e -> submit(EVT_OPEN_CART));
        Button logout = new Button("Sair");
        logout.addActionListener(e -> submit(EVT_LOGOUT));
        header.add(nick);
        header.add(cartBtn);
        header.add(logout);

        contentPane = new Container(new BorderLayout());

        c.add(BorderLayout.NORTH, header);
        c.add(BorderLayout.CENTER, contentPane);
        return c;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        nick.setText("Olá, " + Json.str(st, "nickName"));
        cartBtn.setText("Carrinho (" + Json.intOf(st, "cartItemCount") + ")");

        // tela aninhada (produto/carrinho/recibo) se houver; senão, o painel de produtos
        String content = Json.str(st, "contentViewId");
        String target = !content.isEmpty() ? content : Json.str(st, "productsPanelViewId");

        if (!target.isEmpty() && !target.equals(mountedVsid)) {
            mountedVsid = target;
            Container el = childElement(target);
            contentPane.removeAll();
            if (el != null) {
                contentPane.add(BorderLayout.CENTER, el);
            }
        }
        AbstractCn1View view = childView(target);
        if (view != null) {
            view.doUpdate();
        }
    }
}
