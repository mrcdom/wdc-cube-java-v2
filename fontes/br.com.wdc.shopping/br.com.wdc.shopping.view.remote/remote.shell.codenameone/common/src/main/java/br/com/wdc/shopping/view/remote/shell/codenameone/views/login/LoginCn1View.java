package br.com.wdc.shopping.view.remote.shell.codenameone.views.login;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.Display;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Tela de login (classId {@value #CLASS_ID}) — espelha o design React: dois layouts (expandido com
 * hero à esquerda + card à direita; compacto só com o card), banner azul, "Bem-vindo", labels e
 * hints nos inputs, e a dica de acesso demo.
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
        boolean expanded = Display.getInstance().getDisplayWidth() >= Display.getInstance().convertToPixels(150f);
        Container card = buildCard();

        if (expanded) {
            Container root = new Container(new BorderLayout());
            root.add(BorderLayout.CENTER, buildHero());
            Container right = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
            right.add(card);
            root.add(BorderLayout.EAST, right);
            return root;
        }

        Container root = new Container(new FlowLayout(Component.CENTER, Component.CENTER));
        root.add(card);
        return root;
    }

    // :: Card do formulário (compacto e expandido)

    private Container buildCard() {
        Container card = new Container(BoxLayout.y());
        card.setUIID("LoginCard");
        card.setPreferredW(Display.getInstance().convertToPixels(95f));

        Cn1Dom.render(card, (dom, c) -> {
            // banner azul (logo + título + subtítulo)
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
        });
        return card;
    }

    // :: Hero (layout expandido)

    private Container buildHero() {
        Container hero = new Container(BoxLayout.y());
        hero.setUIID("LoginHero");
        Cn1Dom.render(hero, (dom, h) -> {
            Label logo = dom.label(l -> l.setUIID("HeroTitle"));
            FontImage.setMaterialIcon(logo, FontImage.MATERIAL_SHOPPING_BAG, 14f);
            dom.label(l -> {
                l.setText("WDC Shopping");
                l.setUIID("HeroTitle");
            });
            dom.label(l -> {
                l.setText("Sua compra certa na internet.");
                l.setUIID("HeroSubtitle");
            });
            feature(dom, FontImage.MATERIAL_VERIFIED_USER, "Compra segura");
            feature(dom, FontImage.MATERIAL_LOCAL_SHIPPING, "Entrega rápida");
            feature(dom, FontImage.MATERIAL_AUTORENEW, "Troca garantida");
        });
        return hero;
    }

    private void feature(Cn1Dom dom, char icon, String text) {
        dom.boxX(row -> {
            Label ic = dom.label(l -> l.getAllStyles().setFgColor(0xffffff));
            FontImage.setMaterialIcon(ic, icon, 4f);
            dom.label(l -> {
                l.setText(text);
                l.setUIID("FeatureText");
            });
        });
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
