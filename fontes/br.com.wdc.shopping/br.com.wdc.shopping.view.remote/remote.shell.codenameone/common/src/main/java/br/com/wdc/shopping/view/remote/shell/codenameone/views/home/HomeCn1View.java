package br.com.wdc.shopping.view.remote.shell.codenameone.views.home;

import java.util.Map;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;

import br.com.wdc.shopping.view.remote.shell.codenameone.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.codenameone.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.codenameone.util.Json;

/**
 * Tela principal (classId {@value #CLASS_ID}): app bar (sair + saudação / logo "Shopping" / carrinho
 * com badge) e uma área de conteúdo responsiva:
 * <ul>
 * <li><b>split</b> (padrão): produtos + histórico. No <b>expandido</b>, lado a lado; no
 * <b>compacto</b>, abas (Produtos / Histórico) comutam o painel visível.</li>
 * <li><b>conteúdo interno</b> (produto/carrinho/recibo): ocupa a área toda e <b>esconde as abas</b>
 * — elas pertencem só à Home.</li>
 * </ul>
 */
public class HomeCn1View extends AbstractCn1View {

    public static final String CLASS_ID = "473dbdd7a36a";
    private static final int EVT_LOGOUT = 1;
    private static final int EVT_OPEN_CART = 2;

    /** Largura fixa do painel de histórico no layout expandido. */
    private static final int PURCHASES_WIDTH = 660;

    private Label nick;
    private Label cartBadge;
    private Container body;

    // split (construído uma vez para o modo expandido/compacto corrente)
    private Container splitPane;
    private Container productsHolder;
    private Container purchasesHolder;
    private Container activeHolder; // só no compacto (abas)
    private Button tabProducts;
    private Button tabHistory;
    private boolean showingProducts = true;

    private String mode = "";
    private String mountedContentVsid = "";
    private String mountedProductsVsid = "";
    private String mountedPurchasesVsid = "";

    public HomeCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = new Container(new BorderLayout());
        Cn1Dom.render(root, (dom, r) -> {
            dom.border(BorderLayout.NORTH, bar -> {
                bar.setUIID("AppBar");
                boolean wide = app.isExpanded();

                // esquerda: sair + (saudação só no expandido)
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), BorderLayout.WEST, west -> {
                    Button exit = dom.button(b -> {
                        b.setUIID("AppBarBtn");
                        b.addActionListener(e -> submit(EVT_LOGOUT));
                    });
                    FontImage.setMaterialIcon(exit, FontImage.MATERIAL_LOGOUT, 5f);
                    if (wide) {
                        dom.boxY(greet -> {
                            dom.label(l -> {
                                l.setText("Bem-vindo(a),");
                                l.setUIID("GreetingSmall");
                            });
                            nick = dom.label(l -> l.setUIID("GreetingName"));
                        });
                    }
                });

                // centro: logo + Shopping (+ By WeDoCode só no expandido)
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.CENTER, center -> {
                    Label logo = dom.label(l -> l.setUIID("AppBarLogoBox"));
                    FontImage.setMaterialIcon(logo, FontImage.MATERIAL_SHOPPING_BAG, 5f);
                    dom.boxY(t -> {
                        dom.label(l -> {
                            l.setText("Shopping");
                            l.setUIID("AppBarBrand");
                        });
                        if (wide) {
                            dom.label(l -> {
                                l.setText("By WeDoCode");
                                l.setUIID("AppBarBrandSub");
                            });
                        }
                    });
                });

                // direita: carrinho (texto só no expandido) + badge
                dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), BorderLayout.EAST, east -> {
                    Button cart = dom.button(b -> {
                        if (wide) {
                            b.setText("Carrinho");
                        }
                        b.setUIID("AppBarBtn");
                        b.addActionListener(e -> submit(EVT_OPEN_CART));
                    });
                    FontImage.setMaterialIcon(cart, FontImage.MATERIAL_SHOPPING_CART, 5f);
                    cartBadge = dom.label(l -> l.setUIID("CartBadge"));
                });
            });

            body = dom.border(BorderLayout.CENTER, c -> { });
        });

        buildSplit();
        return root;
    }

    /** Constrói (uma vez) a estrutura do split conforme o modo de largura corrente. */
    private void buildSplit() {
        productsHolder = new Container(new BorderLayout());
        purchasesHolder = new Container(new BorderLayout());

        if (app.isExpanded()) {
            // produtos + histórico lado a lado (sem abas)
            splitPane = new Container(new BorderLayout());
            splitPane.add(BorderLayout.CENTER, productsHolder);
            purchasesHolder.setPreferredW(PURCHASES_WIDTH);
            splitPane.add(BorderLayout.EAST, purchasesHolder);
        } else {
            // abas comutam entre produtos e histórico
            Container tabs = new Container(new GridLayout(2));
            tabs.setUIID("TabNav");
            tabProducts = tabButton("Produtos", FontImage.MATERIAL_VIEW_MODULE, true);
            tabHistory = tabButton("Histórico", FontImage.MATERIAL_HISTORY, false);
            tabs.add(tabProducts);
            tabs.add(tabHistory);

            activeHolder = new Container(new BorderLayout());
            splitPane = new Container(new BorderLayout());
            splitPane.add(BorderLayout.NORTH, tabs);
            splitPane.add(BorderLayout.CENTER, activeHolder);
        }
    }

    private Button tabButton(String text, char icon, boolean products) {
        Button b = new Button(text);
        b.setUIID(products == showingProducts ? "TabItemActive" : "TabItem");
        FontImage.setMaterialIcon(b, icon, 3.5f);
        b.addActionListener(e -> switchTab(products));
        return b;
    }

    private void switchTab(boolean products) {
        if (showingProducts == products) {
            return;
        }
        showingProducts = products;
        refreshTabs();
        update();
    }

    /** Atualiza o estilo das abas e qual painel está montado no holder ativo (compacto). */
    private void refreshTabs() {
        if (activeHolder == null) {
            return; // expandido: sem abas
        }
        tabProducts.setUIID(showingProducts ? "TabItemActive" : "TabItem");
        tabHistory.setUIID(showingProducts ? "TabItem" : "TabItemActive");
        activeHolder.removeAll();
        activeHolder.add(BorderLayout.CENTER, showingProducts ? productsHolder : purchasesHolder);
        activeHolder.revalidate();
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        if (nick != null) {
            nick.setText(Json.str(st, "nickName")); // só existe no expandido
        }
        cartBadge.setText(String.valueOf(Json.intOf(st, "cartItemCount")));

        String content = Json.str(st, "contentViewId");
        if (!content.isEmpty()) {
            showContent(content);
        } else {
            showSplit(st);
        }
    }

    /** Tela aninhada (produto/carrinho/recibo) ocupando tudo — sem abas. */
    private void showContent(String contentVsid) {
        if (!"content".equals(mode) || !contentVsid.equals(mountedContentVsid)) {
            mode = "content";
            mountedContentVsid = contentVsid;
            Container el = childElement(contentVsid);
            body.removeAll();
            if (el != null) {
                body.add(BorderLayout.CENTER, el);
            }
            body.revalidate();
        }
        AbstractCn1View view = childView(contentVsid);
        if (view != null) {
            view.doUpdate();
        }
    }

    /** Split produtos + histórico (lado a lado no expandido, abas no compacto). */
    private void showSplit(Map<String, Object> st) {
        if (!"split".equals(mode)) {
            mode = "split";
            mountedContentVsid = "";
            body.removeAll();
            body.add(BorderLayout.CENTER, splitPane);
            refreshTabs();
            body.revalidate();
        }

        mountInto(productsHolder, Json.str(st, "productsPanelViewId"), false);
        mountInto(purchasesHolder, Json.str(st, "purchasesPanelViewId"), true);

        refreshChild(Json.str(st, "productsPanelViewId"));
        refreshChild(Json.str(st, "purchasesPanelViewId"));
    }

    private void mountInto(Container holder, String childVsid, boolean purchases) {
        if (childVsid.isEmpty()) {
            return;
        }
        String mounted = purchases ? mountedPurchasesVsid : mountedProductsVsid;
        if (childVsid.equals(mounted)) {
            return;
        }
        if (purchases) {
            mountedPurchasesVsid = childVsid;
        } else {
            mountedProductsVsid = childVsid;
        }
        Container el = childElement(childVsid);
        holder.removeAll();
        if (el != null) {
            holder.add(BorderLayout.CENTER, el);
        }
        holder.revalidate();
    }

    private void refreshChild(String childVsid) {
        AbstractCn1View view = childView(childVsid);
        if (view != null) {
            view.doUpdate();
        }
    }
}
