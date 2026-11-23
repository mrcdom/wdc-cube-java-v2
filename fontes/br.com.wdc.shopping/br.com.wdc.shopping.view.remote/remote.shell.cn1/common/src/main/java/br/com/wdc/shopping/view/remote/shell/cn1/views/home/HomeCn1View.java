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
 * Main screen (classId {@value #CLASS_ID}): app bar (sign out + greeting / "Shopping" logo / cart
 * with badge) and a responsive content area:
 * <ul>
 * <li><b>split</b> (default): products + history. In <b>expanded</b> mode, side by side; in
 * <b>compact</b> mode, tabs (Products / History) switch the visible panel.</li>
 * <li><b>inner content</b> (product/cart/receipt): fills the whole area and <b>hides the tabs</b>
 * — they belong to Home only.</li>
 * </ul>
 */
public class HomeCn1View extends AbstractCn1View {

    private static final HomeSel sel = HomeSel.INSTANCE;

    public static final String CLASS_ID = "473dbdd7a36a";
    private static final int EVT_LOGOUT = 1;
    private static final int EVT_OPEN_CART = 2;

    /** Width (mm) of the history panel in the expanded layout — density-independent (see util.Px). */
    private static final float PURCHASES_WIDTH_MM = 65f;

    /** Cart badge color (notification red, over the icon). */
    private static final int BADGE_COLOR = Colors.BADGE;

    private Consumer<String> nick;
    private Container greetBox;   // greeting (west) — visible only when expanded
    private Label brandSub;       // "By WeDoCode" (center) — visible only when expanded
    private Consumer<String> cartText; // cart button text — only when expanded
    private Label cartBadge;
    private Boolean builtExpanded;
    /** Content area: either the nested screen (contentViewId) or the split. */
    private Slot bodySlot;

    // split (rebuilt when switching expanded/compact mode; reuses the panel slots)
    private Container splitPane;
    private Slot productsSlot;
    private Slot purchasesSlot;
    private Slot activeHolder; // compact only (tabs)
    private Button tabProducts;
    private Button tabHistory;
    private boolean showingProducts = true;

    public HomeCn1View(String vsid, BridgeSession session, ShoppingCn1RemoteApp app) {
        super(vsid, session, app);
    }

    @Override
    protected Container build() {
        // panel slots created once (reused in both split modes)
        productsSlot = new Slot();
        purchasesSlot = new Slot();
        Container root = Cn1Dom.render(new BorderLayout(), (dom, r) -> {
            dom.border(BorderLayout.NORTH, bar -> {
                bar.setUIID(sel.APP_BAR);

                // left: sign out + greeting (greeting visibility toggled in doUpdate)
                dom.container(new FlowLayout(Component.LEFT, Component.CENTER), BorderLayout.WEST, west -> {
                    dom.button(b -> {
                        b.setUIID(sel.APP_BAR_BTN);
                        b.addActionListener(e -> submit(EVT_LOGOUT));
                        FontImage.setMaterialIcon(b, FontImage.MATERIAL_LOGOUT, 5f);
                    });
                    greetBox = dom.boxY(greet -> {
                        dom.label(l -> {
                            l.setUIID(sel.GREETING_SMALL);
                            l.setText("Welcome,");
                        });
                        dom.label(l -> {
                            l.setUIID(sel.GREETING_NAME);
                            nick = Guard.text(l);
                        });
                    });
                });

                // center: logo + Shopping (+ By WeDoCode, toggled in doUpdate)
                dom.container(new FlowLayout(Component.CENTER, Component.CENTER), BorderLayout.CENTER, center -> {
                    // BoxLayout.x stretches logo-wrap and brand-wrap to the same height; each wrap centers
                    // its content (valign CENTER) → the brand aligns with the center of the logo icon.
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
                                brandSub = dom.label(l -> {
                                    l.setUIID(sel.APP_BAR_BRAND_SUB);
                                    l.setText("By WeDoCode");
                                });
                            });
                        });
                    });
                });

                // right: cart (text toggled in doUpdate) + badge overlaid on the icon
                dom.container(new FlowLayout(Component.RIGHT, Component.CENTER), BorderLayout.EAST, east -> {
                    dom.container(new LayeredLayout(), null, wrap -> {
                        Button cartBtn = dom.button(b -> {
                            b.setUIID(sel.APP_BAR_BTN);
                            b.addActionListener(e -> submit(EVT_OPEN_CART));
                            FontImage.setMaterialIcon(b, FontImage.MATERIAL_SHOPPING_CART, 5f);
                        });
                        cartText = Guard.text(cartBtn); // Button is-a Label
                        // circular badge via RoundBorder in Java (CN1's CSS border-radius reserves
                        // vertical space); text color/font live in the SCSS.
                        cartBadge = dom.label(l -> {
                            l.setUIID(sel.CART_BADGE);
                            l.getAllStyles().setBgTransparency(0);
                            l.getAllStyles().setBorder(RoundBorder.create().color(BADGE_COLOR));
                        });
                        // pins the badge to the top-right corner, over the icon (insets: top right bottom left)
                        ((LayeredLayout) wrap.getLayout()).setInsets(cartBadge, "0 0 auto auto");
                    });
                });
            });

            bodySlot = dom.slot(BorderLayout.CENTER);
        });
        return root;
    }

    /** (Re)builds the split according to the mode, reusing the panel slots. Called by {@link #layoutSplit()}. */
    private void buildSplit() {
        if (app.isExpanded()) {
            // products + history side by side (no tabs)
            activeHolder = null;
            purchasesSlot.setPreferredW(Px.mm(PURCHASES_WIDTH_MM));
            splitPane = Cn1Dom.render(new BorderLayout(), (dom, pane) -> {
                dom.add(productsSlot, BorderLayout.CENTER);
                dom.add(purchasesSlot, BorderLayout.EAST);
            });
        } else {
            // tabs switch between products and history
            splitPane = Cn1Dom.render(new BorderLayout(), (dom, pane) -> {
                dom.container(new GridLayout(2), BorderLayout.NORTH, tabs -> {
                    tabs.setUIID(sel.TAB_NAV);
                    tabProducts = tabButton(dom, "Products", FontImage.MATERIAL_VIEW_MODULE, true);
                    tabHistory = tabButton(dom, "History", FontImage.MATERIAL_HISTORY, false);
                });
                activeHolder = dom.slot(BorderLayout.CENTER);
            });
            refreshTabs(); // fills the active holder with the initial panel
        }
    }

    /** Rebuilds the split only when the mode (expanded/compact) changes. Reuses the panel slots. */
    private void layoutSplit() {
        boolean expanded = app.isExpanded();
        if (builtExpanded != null && builtExpanded.booleanValue() == expanded) {
            return;
        }
        builtExpanded = Boolean.valueOf(expanded);
        buildSplit();
    }

    private Button tabButton(Cn1Dom dom, String text, char icon, boolean products) {
        return dom.button(b -> {
            b.setUIID(products == showingProducts ? sel.TAB_ITEM_ACTIVE : sel.TAB_ITEM);
            b.setText(text);
            FontImage.setMaterialIcon(b, icon, 3.5f);
            b.addActionListener(e -> switchTab(products));
        });
    }

    private void switchTab(boolean products) {
        if (showingProducts == products) {
            return;
        }
        showingProducts = products;
        refreshTabs();
        // local interaction (no server round trip): marks dirty only the panel that became visible
        // — e.g.: the history recomputes its capacity now that it has a height.
        Map<String, Object> st = state();
        AbstractCn1View panel = childView(Json.str(st, products ? "productsPanelViewId" : "purchasesPanelViewId"));
        if (panel != null) {
            app.markDirty(panel);
        }
    }

    /** Updates the tab styles and which panel is mounted in the active holder (compact). */
    private void refreshTabs() {
        if (activeHolder == null) {
            return; // expanded: no tabs
        }
        tabProducts.setUIID(showingProducts ? sel.TAB_ITEM_ACTIVE : sel.TAB_ITEM);
        tabHistory.setUIID(showingProducts ? sel.TAB_ITEM : sel.TAB_ITEM_ACTIVE);
        activeHolder.mount(showingProducts ? productsSlot : purchasesSlot);
    }

    @Override
    public void doUpdate() {
        boolean wide = app.isExpanded();
        layoutSplit();                       // rebuilds the split if the mode changed
        Guard.visible(greetBox, wide);       // greeting / "By WeDoCode" / cart text: only when expanded
        Guard.visible(brandSub, wide);
        cartText.accept(wide ? "Cart" : "");

        Map<String, Object> st = state();
        nick.accept(Json.str(st, "nickName"));
        int cartCount = Json.intOf(st, "cartItemCount");
        cartBadge.setText(String.valueOf(cartCount));
        Guard.visible(cartBadge, cartCount > 0); // badge hides when the cart is empty (convention)

        String content = Json.str(st, "contentViewId");
        if (!content.isEmpty()) {
            // nested screen (product/cart/receipt) fills everything — hides the tabs
            bodySlot.mount(childElement(content));
        } else {
            // products + history split (side by side when expanded, tabs when compact)
            bodySlot.mount(splitPane);
            productsSlot.mount(childElement(Json.str(st, "productsPanelViewId")));
            purchasesSlot.mount(childElement(Json.str(st, "purchasesPanelViewId")));
        }
        // the children's doUpdate (content/panels) is dispatched by the bridge (ViewState received)

    }
}
