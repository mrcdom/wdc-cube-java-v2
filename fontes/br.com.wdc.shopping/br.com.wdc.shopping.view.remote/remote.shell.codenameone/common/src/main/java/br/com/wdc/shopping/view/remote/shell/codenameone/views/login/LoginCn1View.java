package br.com.wdc.shopping.view.remote.shell.codenameone.views.login;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BoxLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Tela de login (classId {@value #CLASS_ID}) — card com banner azul, "Bem-vindo", labels e hints
 * nos inputs e a dica de acesso demo (espelha o card do design React). Centralização/largura e o
 * hero do layout expandido são calibrados em seguida.
 */
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
        Container root = new Container(BoxLayout.y()); // transparente; o card é filho
        Cn1Dom.render(root, (dom, r) -> dom.boxY(card -> {
            card.setUIID("LoginCard");

            // banner azul: logo + título + subtítulo
            dom.boxY(banner -> {
                banner.setUIID("LoginBanner");
                Label logo = dom.label(l -> l.setUIID("LogoBox"));
                FontImage.setMaterialIcon(logo, FontImage.MATERIAL_SHOPPING_BAG, 7f);
                dom.label(l -> {
                    l.setText("WDC Shopping");
                    l.setUIID("BannerTitle");
                });
                dom.label(l -> {
                    l.setText("Sua compra certa na internet.");
                    l.setUIID("BannerSubtitle");
                });
            });

            // boas-vindas
            dom.label(l -> {
                l.setText("Bem-vindo");
                l.setUIID("WelcomeTitle");
            });
            dom.label(l -> {
                l.setText("Entre com suas credenciais para continuar");
                l.setUIID("WelcomeSubtitle");
            });

            // erro
            error = dom.label(l -> {
                l.setUIID("WelcomeSubtitle");
                l.getAllStyles().setFgColor(0xcc0000);
            });

            // usuário
            dom.label(l -> {
                l.setText("Usuário");
                l.setUIID("FieldLabel");
            });
            user = dom.textField(tf -> tf.setHint("Digite seu usuário"));

            // senha
            dom.label(l -> {
                l.setText("Senha");
                l.setUIID("FieldLabel");
            });
            pass = dom.textField(tf -> {
                tf.setHint("Digite sua senha");
                tf.setConstraint(TextArea.PASSWORD);
            });

            // loading
            loading = dom.label(l -> {
                l.setText("Entrando...");
                l.setUIID("WelcomeSubtitle");
            });

            // entrar
            dom.button(b -> {
                b.setText("Entrar");
                b.setUIID("PrimaryButton");
                b.addActionListener(e -> doLogin());
            });

            // dica de acesso demo
            dom.boxY(hint -> {
                hint.setUIID("DemoHint");
                dom.label(l -> {
                    l.setText("Acesso demo: admin / admin");
                    l.setUIID("DemoText");
                });
            });
        }));
        return root;
    }

    private void doLogin() {
        Map<String, Object> form = new HashMap<>();
        form.put("p.userName", user.getText());
        form.put("p.password", session.cipher(pass.getText()));
        submit(EVT_LOGIN, form);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        visible(loading, Json.boolOf(st, "loading"));
        int errorCode = Json.intOf(st, "errorCode");
        error.setText(errorCode != 0 ? "Usuário ou senha inválidos." : "");
        visible(error, errorCode != 0);
    }
}
