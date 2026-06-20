package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Tela principal (classId {@value #CLASS_ID}): app bar (sair + saudação / logo "Shopping" / carrinho
 * com badge) e uma área de conteúdo — painel de produtos por padrão, ou uma tela aninhada
 * (produto/carrinho/recibo) quando o servidor define {@code contentViewId}.
 */
public class HomeCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "473dbdd7a36a";
    private static final int EVT_LOGOUT = 1;
    private static final int EVT_OPEN_CART = 2;

    private Label nick;
    private Label cartBadge;
    private Container contentPane;
    private String mountedVsid = "";

    public HomeCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(new BorderLayout());
        Cn1Dom.render(root, (dom, r) -> {
            dom.border(BorderLayout.NORTH, bar -> {
                bar.setUIID("AppBar");

                // esquerda: sair + saudação
                dom.boxX(BorderLayout.WEST, west -> {
                    Button exit = dom.button(b -> {
                        b.setUIID("AppBarBtn");
                        b.addActionListener(e -> submit(EVT_LOGOUT));
                    });
                    FontImage.setMaterialIcon(exit, FontImage.MATERIAL_LOGOUT, 5f);
                    dom.boxY(greet -> {
                        dom.label(l -> {
                            l.setText("Bem-vindo(a),");
                            l.setUIID("GreetingSmall");
                        });
                        nick = dom.label(l -> l.setUIID("GreetingName"));
                    });
                });

                // centro: logo + Shopping / By WeDoCode
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.CENTER, center -> {
                    Label logo = dom.label(l -> l.setUIID("AppBarLogoBox"));
                    FontImage.setMaterialIcon(logo, FontImage.MATERIAL_SHOPPING_BAG, 5f);
                    dom.boxY(t -> {
                        dom.label(l -> {
                            l.setText("Shopping");
                            l.setUIID("AppBarBrand");
                        });
                        dom.label(l -> {
                            l.setText("By WeDoCode");
                            l.setUIID("AppBarBrandSub");
                        });
                    });
                });

                // direita: carrinho + badge
                dom.boxX(BorderLayout.EAST, east -> {
                    Button cart = dom.button(b -> {
                        b.setText("Carrinho");
                        b.setUIID("AppBarBtn");
                        b.addActionListener(e -> submit(EVT_OPEN_CART));
                    });
                    FontImage.setMaterialIcon(cart, FontImage.MATERIAL_SHOPPING_CART, 5f);
                    cartBadge = dom.label(l -> l.setUIID("CartBadge"));
                });
            });

            contentPane = dom.border(BorderLayout.CENTER, c -> { });
        });
        return root;
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        nick.setText(Json.str(st, "nickName"));
        cartBadge.setText(String.valueOf(Json.intOf(st, "cartItemCount")));

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
