package br.com.wdc.shopping.view.remote.shell.codenameone.views.login;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/** Tela de login (classId {@value #CLASS_ID}). */
public class LoginCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "c677cda52d14";
    private static final int EVT_LOGIN = 1;

    private TextField user;
    private TextField pass;
    private Label loading;
    private Label error;

    public LoginCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(BoxLayout.y());
        Cn1Dom.render(root, (dom, r) -> {
            dom.label(l -> l.setText("Entrar"));
            user = dom.textField(tf -> tf.setHint("Usuário"));
            pass = dom.textField(tf -> {
                tf.setHint("Senha");
                tf.setConstraint(TextArea.PASSWORD);
            });
            loading = dom.label(l -> l.setText("Entrando..."));
            error = dom.label(l -> l.getAllStyles().setFgColor(0xcc0000));
            dom.button(b -> {
                b.setText("Entrar");
                b.addActionListener(e -> {
                    Map<String, Object> form = new HashMap<>();
                    form.put("p.userName", user.getText());
                    form.put("p.password", session.cipher(pass.getText()));
                    submit(EVT_LOGIN, form);
                });
            });
        });
        return root;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        visible(loading, Json.boolOf(st, "loading"));
        int errorCode = Json.intOf(st, "errorCode");
        error.setText(errorCode != 0 ? "Falha no login (código " + errorCode + ")" : "");
        visible(error, errorCode != 0);
    }
}
