package br.com.wdc.shopping.view.remote.shell.cn1.views.home;

import java.util.Map;
import java.util.function.Consumer;

import com.codename1.ui.Button;
import com.codename1.ui.Component;
import com.codename1.ui.Container;
import com.codename1.ui.FontImage;
import com.codename1.ui.Label;
import com.codename1.ui.layouts.BorderLayout;
import com.codename1.ui.layouts.FlowLayout;
import com.codename1.ui.layouts.GridLayout;
import com.codename1.ui.layouts.LayeredLayout;
import com.codename1.ui.plaf.RoundBorder;

import br.com.wdc.shopping.view.remote.shell.cn1.ShoppingCn1RemoteApp;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.AbstractCn1View;
import br.com.wdc.shopping.view.remote.shell.cn1.bridge.BridgeSession;
import br.com.wdc.shopping.view.remote.shell.cn1.theme.Colors;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Cn1Dom;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Guard;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Json;
import br.com.wdc.shopping.view.remote.shell.cn1.util.Px;
import br.com.wdc.shopping.view.remote.shell.cn1.widgets.Slot;

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

    private static final HomeSel sel = HomeSel.INSTANCE;

    public static final String CLASS_ID = "473dbdd7a36a";
    private static final int EVT_LOGOUT = 1;
    private static final int EVT_OPEN_CART = 2;

    /** Largura (mm) do painel de histórico no layout expandido — densidade-independente (ver util.Px). */
    private static final float PURCHASES_WIDTH_MM = 80f;

    /** Cor do badge do carrinho (vermelho de notificação, sobre o ícone). */
    private static final int BADGE_COLOR = Colors.BADGE;

    private Consumer<String> nick;
    private Label cartBadge;
    /** Área de conteúdo: ou a tela aninhada (contentViewId) ou o split (chave local "split"). */
    private Slot bodySlot;

    // split (construído uma vez para o modo expandido/compacto corrente)
    private Container splitPane;
    private Slot productsSlot;
    private Slot purchasesSlot;
    private Slot activeHolder; // só no compacto (abas)
    private Button tabProducts;
    private Button tabHistory;
    private boolean showingProducts = true;

    public HomeCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        Container root = Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            dom.border(BorderLayout.NORTH, bar -> {
                bar.setUIID(sel.APP_BAR);
                boolean wide = app.isExpanded();

                // esquerda: sair + (saudação só no expandido)
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), BorderLayout.WEST, west -> {
                    dom.button(b -> {
                        b.setUIID(sel.APP_BAR_BTN);
                        b.addActionListener(e -> submit(EVT_LOGOUT));
                        FontImage.setMaterialIcon(b, FontImage.MATERIAL_LOGOUT, 5f);
                    });
                    if (wide) {
                        dom.boxY(greet -> {
                            dom.label(l -> {
                                l.setUIID(sel.GREETING_SMALL);
                                l.setText("Bem-vindo(a),");
                            });
                            dom.label(l -> {
                                l.setUIID(sel.GREETING_NAME);
                                nick = Guard.text(l);
                            });
                        });
                    }
                });

                // centro: logo + Shopping (+ By WeDoCode só no expandido)
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.CENTER, center -> {
                    // BoxLayout.x estica logo-wrap e marca-wrap à mesma altura; cada wrap centraliza
                    // seu conteúdo (valign CENTER) → a marca alinha pelo centro do ícone do logo.
                    dom.boxX(group -> {
                        dom.container(new FlowLayout(Component.CENTER, Component.CENTER), null, logoWrap -> {
                            dom.label(l -> {
                                l.setUIID(sel.APP_BAR_LOGO_BOX);
                                FontImage.setMaterialIcon(l, FontImage.MATERIAL_SHOPPING_BAG, 5f);
                            });
                        });
                        dom.container(new FlowLayout(Component.LEFT, Component.CENTER), null, brandWrap -> {
                            dom.boxY(t -> {
                                dom.label(l -> {
                                    l.setUIID(sel.APP_BAR_BRAND);
                                    l.setText("Shopping");
                                });
                                if (wide) {
                                    dom.label(l -> {
                                        l.setUIID(sel.APP_BAR_BRAND_SUB);
                                        l.setText("By WeDoCode");
                                    });
                                }
                            });
                        });
                    });
                });

                // direita: carrinho (texto só no expandido) + badge sobreposto ao ícone
                dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), BorderLayout.EAST, east -> {
                    Container cartWrap = dom.container(new LayeredLayout(), null, wrap -> {
                        dom.button(b -> {
                            b.setUIID(sel.APP_BAR_BTN);
                            if (wide) {
                                b.setText("Carrinho");
                            }
                            b.addActionListener(e -> submit(EVT_OPEN_CART));
                            FontImage.setMaterialIcon(b, FontImage.MATERIAL_SHOPPING_CART, 5f);
                        });
                        // badge circular via RoundBorder no Java (o border-radius do CSS do CN1 reserva
                        // espaço vertical); cor/fonte do texto ficam no SCSS.
                        cartBadge = dom.label(l -> l.setUIID(sel.CART_BADGE));
                        cartBadge.getAllStyles().setBgTransparency(0);
                        cartBadge.getAllStyles().setBorder(RoundBorder.create().color(BADGE_COLOR));
                    });
                    // fixa o badge no canto superior direito, sobre o ícone (insets: top right bottom left)
                    ((LayeredLayout) cartWrap.getLayout()).setInsets(cartBadge, "0 0 auto auto");
                });
            });
        });

        bodySlot = new Slot();
        root.add(BorderLayout.CENTER, bodySlot);

        buildSplit();
        return root;
    }

    /** Constrói (uma vez) a estrutura do split conforme o modo de largura corrente. */
    private void buildSplit() {
        productsSlot = new Slot();
        purchasesSlot = new Slot();

        if (app.isExpanded()) {
            // produtos + histórico lado a lado (sem abas)
            splitPane = new Container(new BorderLayout());
            splitPane.add(BorderLayout.CENTER, productsSlot);
            purchasesSlot.setPreferredW(Px.mm(PURCHASES_WIDTH_MM));
            splitPane.add(BorderLayout.EAST, purchasesSlot);
        } else {
            // abas comutam entre produtos e histórico
            Container tabs = new Container(new GridLayout(2));
            tabs.setUIID(sel.TAB_NAV);
            tabProducts = tabButton("Produtos", FontImage.MATERIAL_VIEW_MODULE, true);
            tabHistory = tabButton("Histórico", FontImage.MATERIAL_HISTORY, false);
            tabs.add(tabProducts);
            tabs.add(tabHistory);

            activeHolder = new Slot();
            splitPane = new Container(new BorderLayout());
            splitPane.add(BorderLayout.NORTH, tabs);
            splitPane.add(BorderLayout.CENTER, activeHolder);
            refreshTabs(); // popula o holder ativo com o painel inicial (produtos)
        }
    }

    private Button tabButton(String text, char icon, boolean products) {
        Button b = new Button(text);
        b.setUIID(products == showingProducts ? sel.TAB_ITEM_ACTIVE : sel.TAB_ITEM);
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
        // interação local (sem ida ao servidor): marca dirty apenas o painel que ficou visível
        // — ex.: o histórico recalcula a capacidade agora que tem altura.
        Map<String, Object> st = state();
        AbstractCn1View panel = childView(Json.str(st, products ? "productsPanelViewId" : "purchasesPanelViewId"));
        if (panel != null) {
            app.markDirty(panel);
        }
    }

    /** Atualiza o estilo das abas e qual painel está montado no holder ativo (compacto). */
    private void refreshTabs() {
        if (activeHolder == null) {
            return; // expandido: sem abas
        }
        tabProducts.setUIID(showingProducts ? sel.TAB_ITEM_ACTIVE : sel.TAB_ITEM);
        tabHistory.setUIID(showingProducts ? sel.TAB_ITEM : sel.TAB_ITEM_ACTIVE);
        activeHolder.mount(showingProducts ? productsSlot : purchasesSlot);
    }

    @Override
    public void doUpdate() {
        Map<String, Object> st = state();
        if (nick != null) {
            nick.accept(Json.str(st, "nickName")); // só existe no expandido
        }
        int cartCount = Json.intOf(st, "cartItemCount");
        cartBadge.setText(String.valueOf(cartCount));
        Guard.visible(cartBadge, cartCount > 0); // badge some quando o carrinho está vazio (convenção)

        String content = Json.str(st, "contentViewId");
        if (!content.isEmpty()) {
            // tela aninhada (produto/carrinho/recibo) ocupa tudo — esconde as abas
            bodySlot.mount(childElement(content));
        } else {
            // split produtos + histórico (lado a lado no expandido, abas no compacto)
            bodySlot.mount(splitPane);
            productsSlot.mount(childElement(Json.str(st, "productsPanelViewId")));
            purchasesSlot.mount(childElement(Json.str(st, "purchasesPanelViewId")));
        }
        // o doUpdate das filhas (conteúdo/painéis) é despachado pela bridge (ViewState recebido)
    }
}
