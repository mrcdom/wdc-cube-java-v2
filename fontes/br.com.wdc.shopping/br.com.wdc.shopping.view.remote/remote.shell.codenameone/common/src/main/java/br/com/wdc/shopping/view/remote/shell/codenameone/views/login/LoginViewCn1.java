package br.com.wdc.shopping.view.remote.shell.codenameone.views.login;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Container;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractViewCn1;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/** Tela de login (classId {@value #CLASS_ID}). */
public class LoginViewCn1 extends AbstractViewCn1 {

    public static final String CLASS_ID = "c677cda52d14";
    private static final int EVT_LOGIN = 1;

    private TextField user;
    private TextField pass;
    private Label loading;
    private Label error;

    public LoginViewCn1(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container c = new Container(BoxLayout.y());
        c.add(new Label("Entrar"));

        user = new TextField();
        user.setHint("Usuário");
        pass = new TextField();
        pass.setHint("Senha");
        pass.setConstraint(TextArea.PASSWORD);

        loading = new Label("Entrando...");
        error = new Label("");
        error.getAllStyles().setFgColor(0xcc0000);

        Button login = new Button("Entrar");
        login.addActionListener(e -> {
            Map<String, Object> form = new HashMap<>();
            form.put("p.userName", user.getText());
            form.put("p.password", session.cipher(pass.getText()));
            submit(EVT_LOGIN, form);
        });

        c.add(user);
        c.add(pass);
        c.add(loading);
        c.add(error);
        c.add(login);
        return c;
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
