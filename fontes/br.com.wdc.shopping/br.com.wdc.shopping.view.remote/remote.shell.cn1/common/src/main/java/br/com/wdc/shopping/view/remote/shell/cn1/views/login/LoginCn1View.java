package br.com.wdc.shopping.view.remote.shell.cn1.views.login;

import java.util.HashMap;
import java.util.Map;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Decor;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;

/**
 * Tela de login (classId {@value #CLASS_ID}) — espelha o design React: layout EXPANDIDO (hero azul
 * à esquerda + card à direita) ou COMPACTO (só o card), conforme a largura. Card com banner azul,
 * "Bem-vindo", labels e hints nos inputs e a dica de acesso demo.
 */
public class LoginCn1View extends AbstractCn1View {

    private static final LoginSel sel = LoginSel.INSTANCE;

    public static final String CLASS_ID = "c677cda52d14";
    private static final int EVT_LOGIN = 1;
    /** Largura (mm) do card do formulário no expandido — densidade-independente (ver util.Px). */
    private static final float CARD_WIDTH_MM = 90f;

    private TextField user;
    private TextField pass;
    private Label loading;
    private Label error;

    public LoginCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container card = buildCard();

        if (app.isExpanded()) {
            card.setPreferredW(Px.mm(CARD_WIDTH_MM));
            return Cn1Dom.render(new BorderLayout(), (dom, root) -> {
                dom.add(buildHero(), BorderLayout.CENTER);
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.EAST,
                        wrap -> dom.add(card, null));
            });
        }

        // compacto: card ocupa a largura, no topo
        return Cn1Dom.render(BoxLayout.y(), (dom, root) -> dom.add(card, null));
    }

    // :: Card do formulário

    private Container buildCard() {
        return Cn1Dom.render(BoxLayout.y(), (dom, c) -> {
            c.setUIID(sel.LOGIN_CARD);
            // banner azul: logo (caixa pequena centralizada) + título + subtítulo
            dom.boxY(banner -> {
                banner.setUIID(sel.LOGIN_BANNER);
                Decor.blueWithCircles(banner);
                dom.container(new FlowLayout(Component.CENTER), null, row -> {
                    dom.label(l -> {
                        l.setUIID(sel.LOGO_BOX);
                        FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 7f);
                    });
                });
                dom.label(l -> {
                    l.setText("WDC Shopping");
                    l.setUIID(sel.BANNER_TITLE);
                });
                dom.spanLabel(l -> {
                    l.setText("Sua compra certa na internet.");
                    l.setTextUIID(sel.BANNER_SUBTITLE);
                });
            });

            // boas-vindas
            dom.label(l -> {
                l.setText("Bem-vindo");
                l.setUIID(sel.WELCOME_TITLE);
            });
            dom.spanLabel(l -> {
                l.setText("Entre com suas credenciais para continuar");
                l.setTextUIID(sel.WELCOME_SUBTITLE);
            });

            // erro
            error = dom.label(l -> {
                l.setUIID(sel.WELCOME_SUBTITLE);
                l.getAllStyles().setFgColor(0xcc0000);
            });

            // usuário
            dom.label(l -> {
                l.setText("Usuário");
                l.setUIID(sel.FIELD_LABEL);
            });
            user = dom.textField(tf -> tf.setHint("Digite seu usuário"));

            // senha
            dom.label(l -> {
                l.setText("Senha");
                l.setUIID(sel.FIELD_LABEL);
            });
            pass = dom.textField(tf -> {
                tf.setHint("Digite sua senha");
                tf.setConstraint(TextArea.PASSWORD);
                tf.setDoneListener(e -> doLogin()); // Enter no campo dispara o login
            });

            // loading
            loading = dom.label(l -> {
                l.setText("Entrando...");
                l.setUIID(sel.WELCOME_SUBTITLE);
            });

            // entrar
            dom.button(b -> {
                b.setText("Entrar");
                b.setUIID(sel.PRIMARY_BUTTON);
                b.addActionListener(e -> doLogin());
            });

            // dica de acesso demo
            dom.boxY(hint -> {
                hint.setUIID(sel.DEMO_HINT);
                dom.spanLabel(l -> {
                    l.setText("Acesso demo: admin / admin");
                    l.setTextUIID(sel.DEMO_TEXT);
                });
            });
        });
    }

    // :: Hero (layout expandido)

    private Container buildHero() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, hero) -> {
            hero.setUIID(sel.LOGIN_HERO);
            Decor.blueWithCircles(hero);

            dom.boxY(content -> {
                dom.container(new FlowLayout(Component.CENTER), null, row -> {
                    dom.label(l -> {
                        l.getAllStyles().setFgColor(0xffffff);
                        FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 14f);
                    });
                });
                dom.label(l -> {
                    l.setText("WDC Shopping");
                    l.setUIID(sel.HERO_TITLE);
                });
                dom.spanLabel(l -> {
                    l.setText("Sua compra certa na internet.");
                    l.setTextUIID(sel.HERO_SUBTITLE);
                });
                feature(dom, FontImage.MATERIAL_VERIFIED_USER, "Compra segura");
                feature(dom, FontImage.MATERIAL_LOCAL_SHIPPING, "Entrega rápida");
                feature(dom, FontImage.MATERIAL_AUTORENEW, "Troca garantida");
            });
        });
    }

    private void feature(Cn1Dom dom, char icon, String text) {
        dom.boxX(row -> {
            dom.label(l -> {
                l.getAllStyles().setFgColor(0xffffff);
                FontImage.setMaterialIcon(l, icon, 4f);
            });
            dom.label(l -> {
                l.setText(text);
                l.setUIID(sel.FEATURE_TEXT);
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
