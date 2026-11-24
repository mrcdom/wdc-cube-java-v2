package br.com.wdc.store.view.remote.shell.cn1.views.login;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.TextArea;
import com.codename1.ui.TextField;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.BoxLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.store.view.remote.shell.cn1.StoreCn1RemoteApp;
import br.com.wdc.store.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.store.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.store.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.store.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.store.view.remote.shell.cn1.util.Decor;
import br.com.wdc.store.view.remote.shell.cn1.util.Guard;
import br.com.wdc.store.view.remote.shell.cn1.util.Json;
import br.com.wdc.store.view.remote.shell.cn1.util.Px;
import br.com.wdc.store.view.remote.shell.cn1.widgets.Slot;

/**
 * Login screen (classId {@value #CLASS_ID}) — mirrors the React design: EXPANDED layout (blue hero
 * on the left + card on the right) or COMPACT (card only), depending on the width. Card with a blue
 * banner, "Welcome", labels and hints on the inputs and the demo access hint.
 */
public class LoginCn1View extends AbstractCn1View {

    private static final LoginSel sel = LoginSel.INSTANCE;

    public static final String CLASS_ID = "c677cda52d14";
    private static final int EVT_LOGIN = 1;
    /** Width (mm) of the form card when expanded — density-independent (see util.Px). */
    private static final float CARD_WIDTH_MM = 90f;

    private Slot shell;
    private Container card;
    private Container hero;
    private Boolean builtExpanded;
    private TextField user;
    private TextField pass;
    private Label loading;
    private Label error;
    private Consumer<String> errorText;

    public LoginCn1View(String vsid, BridgeSession session, StoreCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        // pieces built ONCE (fields bound here) and reused in both modes; the arrangement happens in doUpdate
        card = buildCard();
        shell = new Slot();
        return shell;
    }

    /**
     * Arranges the pieces according to the mode (expanded: hero + card; compact: card only), re-mounting
     * only when the mode <b>changes</b>. Since it reuses the {@code card}/{@code hero}, it preserves the
     * state (e.g.: already typed text) across a rotation — unlike rebuilding from scratch.
     */
    private void layout() {
        boolean expanded = app.isExpanded();
        if (builtExpanded != null && builtExpanded.booleanValue() == expanded) {
            return;
        }
        builtExpanded = Boolean.valueOf(expanded);
        if (expanded) {
            if (hero == null) {
                hero = buildHero();
            }
            card.setPreferredW(Px.mm(CARD_WIDTH_MM));
            shell.mount(Cn1Dom.render(new BorderLayout(), (dom, root) -> {
                dom.add(hero, BorderLayout.CENTER);
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.EAST,
                        wrap -> dom.add(card, null));
            }));
        } else {
            // compact: the card fills the width, at the top (BoxLayout.y stretches → ignores the expanded preferredW)
            shell.mount(Cn1Dom.render(BoxLayout.y(), (dom, root) -> dom.add(card, null)));
        }
    }

    // :: Form card

    private Container buildCard() {
        return Cn1Dom.render(BoxLayout.y(), (dom, c) -> {
            c.setUIID(sel.LOGIN_CARD);
            // blue banner: logo (small centered box) + title + subtitle
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
                    l.setUIID(sel.BANNER_TITLE);
                    l.setText("WDC Store");
                });
                dom.spanLabel(l -> {
                    l.setTextUIID(sel.BANNER_SUBTITLE);
                    l.setText("Your trusted online store.");
                });
            });

            // welcome
            dom.label(l -> {
                l.setUIID(sel.WELCOME_TITLE);
                l.setText("Welcome");
            });
            dom.spanLabel(l -> {
                l.setTextUIID(sel.WELCOME_SUBTITLE);
                l.setText("Sign in with your credentials to continue");
            });

            // error
            error = dom.label(l -> {
                l.setUIID(sel.WELCOME_SUBTITLE);
                l.getAllStyles().setFgColor(Colors.ERROR);
                errorText = Guard.text(l);
            });

            // username
            dom.label(l -> {
                l.setUIID(sel.FIELD_LABEL);
                l.setText("Username");
            });
            user = dom.textField(tf -> tf.setHint("Enter your username"));

            // password
            dom.label(l -> {
                l.setUIID(sel.FIELD_LABEL);
                l.setText("Password");
            });
            pass = dom.textField(tf -> {
                tf.setHint("Enter your password");
                tf.setConstraint(TextArea.PASSWORD);
                tf.setDoneListener(e -> doLogin()); // Enter in the field triggers the login
            });

            // loading
            loading = dom.label(l -> {
                l.setUIID(sel.WELCOME_SUBTITLE);
                l.setText("Signing in...");
            });

            // sign in
            dom.button(b -> {
                b.setUIID(sel.PRIMARY_BUTTON);
                b.setText("Sign in");
                b.addActionListener(e -> doLogin());
            });

            // demo access hint
            dom.boxY(hint -> {
                hint.setUIID(sel.DEMO_HINT);
                dom.spanLabel(l -> {
                    l.setTextUIID(sel.DEMO_TEXT);
                    l.setText("Demo access: admin / admin");
                });
            });
        });
    }

    // :: Hero (expanded layout)

    private Container buildHero() {
        return Cn1Dom.render(new FlowLayout(Component.CENTER, Component.CENTER), (dom, hero) -> {
            hero.setUIID(sel.LOGIN_HERO);
            Decor.blueWithCircles(hero);

            dom.boxY(content -> {
                dom.container(new FlowLayout(Component.CENTER), null, row -> {
                    dom.label(l -> {
                        l.getAllStyles().setFgColor(Colors.SURFACE);
                        FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 14f);
                    });
                });
                dom.label(l -> {
                    l.setUIID(sel.HERO_TITLE);
                    l.setText("WDC Store");
                });
                dom.spanLabel(l -> {
                    l.setTextUIID(sel.HERO_SUBTITLE);
                    l.setText("Your trusted online store.");
                });
                feature(dom, FontImage.MATERIAL_VERIFIED_USER, "Secure checkout");
                feature(dom, FontImage.MATERIAL_LOCAL_SHIPPING, "Fast delivery");
                feature(dom, FontImage.MATERIAL_AUTORENEW, "Hassle-free returns");
            });
        });
    }

    private void feature(Cn1Dom dom, char icon, String text) {
        dom.boxX(row -> {
            dom.label(l -> {
                l.getAllStyles().setFgColor(Colors.SURFACE);
                FontImage.setMaterialIcon(l, icon, 4f);
            });
            dom.label(l -> {
                l.setUIID(sel.FEATURE_TEXT);
                l.setText(text);
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
        layout(); // adapts the structure to the current mode (re-mounts only on a switch)
        Map<String, Object> scope = scope();
        Guard.visible(loading, Json.boolOf(scope, "loading"));
        boolean hasError = Json.intOf(scope, "errorCode") != 0;
        errorText.accept(hasError ? "Invalid username or password."
 : "");
        Guard.visible(error, hasError);
    }
}
