package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spTheme;
import static br.com.wdc.framework.vdom.VNode.button;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.nav;
import static br.com.wdc.framework.vdom.VNode.slot;
import static br.com.wdc.framework.vdom.VNode.span;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Home view: navbar + content panels. State fields: nickName, cartItemCount, productsPanelViewId, purchasesPanelViewId,
 * contentViewId, errorMessage.
 */
public class HomeView extends AbstractRemoteView {

    public static final String VIEW_ID = "473dbdd7a36a";

    private static final int ON_EXIT = 1;
    private static final int ON_OPEN_CART = 2;

    @SuppressWarnings({ "java:S1214", "static-access" })
    private interface Css {
        CssUtility u = CssUtility.INSTANCE;
        CssComponents c = CssComponents.INSTANCE;
        CssIcons icon = CssIcons.INSTANCE;

        String HIDDEN = u.HIDDEN;
        String ROOT = clsx(u.FLEX_COL, u.FLEX_GROW, u.FLEX_1, u.MIN_H_0, u.OVERFLOW_HIDDEN);
        String ERROR_VISIBLE = c.ALERT_ERROR;
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;

        // Navbar
        String NAVBAR = "navbar";
        String NAV_GROUP = "nav-group";
        String NAV_RIGHT = u.FLEX_ITEMS_CENTER;
        String EXIT_ICON = clsx(icon.BOX_ARROW_LEFT, u.TEXT_LG, u.TEXT_WHITE);
        String GREETING_WRAP = clsx(u.SM_SHOW, u.FLEX_COL, u.LEADING_TIGHT);
        String GREETING_LABEL = clsx(u.TEXT_XS, u.TEXT_WHITE_70, u.FONT_NORMAL);
        String GREETING_NAME = clsx(u.TEXT_SM, u.FONT_SEMIBOLD, u.TEXT_WHITE);
        String LOGO_BOX = "logo-box";
        String LOGO_ICON = clsx(icon.BAG_CHECK, u.TEXT_XL, u.TEXT_WHITE);
        String LOGO_TEXT_WRAP = clsx(u.FLEX_COL, u.LEADING_TIGHT);
        String LOGO_TITLE = clsx(u.TEXT_BASE, u.FONT_BOLD, u.TEXT_WHITE, u.TRACKING_TIGHT);
        String LOGO_SUBTITLE = clsx(u.SM_SHOW, u.TEXT_XS, u.TEXT_WHITE_65, u.FONT_NORMAL, u.TRACKING_WIDE);
        String CART_BTN = u.RELATIVE;
        String CART_ICON = clsx(icon.BAG, u.TEXT_XL, u.TEXT_WHITE);
        String CART_LABEL = clsx(u.SM_SHOW, u.TEXT_SM, u.TEXT_WHITE, u.FONT_MEDIUM, u.ML_6);
        String CART_BADGE = "cart-badge";

        // Content pane
        String CONTENT_OVERLAY = clsx(u.FLEX_COL, u.FLEX_GROW, u.OVERFLOW_AUTO, u.MIN_H_0, u.BG_DEFAULT);
        String SPLIT_COL = clsx(u.FLEX_COL, u.FLEX_GROW, u.MIN_H_0, u.OVERFLOW_HIDDEN);
        String SPLIT_ROW = clsx(u.MD_ROW, u.FLEX, u.FLEX_GROW, u.OVERFLOW_AUTO, u.MIN_H_0, u.BG_DEFAULT);
        String PANEL_SLOT = clsx(u.FLEX_COL, u.FLEX_GROW, u.H_FULL);

        // Tab nav
        String TAB_NAV = clsx(u.MD_HIDE, "tab-nav");
        String TAB_ACTIVE = "tab-item tab-item--active";
        String TAB_INACTIVE = "tab-item tab-item--inactive";
        String TAB_INDICATOR = "tab-indicator";
        String TAB_ICON_PRODUCTS = clsx(icon.GRID_3X3_GAP, u.TEXT_BASE);
        String TAB_ICON_HISTORY = clsx(icon.CLOCK_HISTORY, u.TEXT_BASE);
    }

    // Stable event listeners (avoid re-registration on every render)
    private final EventListener<Event> onExit = evt -> submit(ON_EXIT);
    private final EventListener<Event> onOpenCart = evt -> submit(ON_OPEN_CART);
    private final EventListener<Event> onTabProducts = evt -> switchTab(true);
    private final EventListener<Event> onTabHistory = evt -> switchTab(false);

    private boolean showingProducts = true;

    public HomeView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        var nickName = scope.getString("nickName", "");
        var cartCount = String.valueOf(scope.getInt("cartItemCount"));
        var errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        // Child view elements
        var productsPanelVsid = scope.getString("productsPanelViewId");
        var purchasesPanelVsid = scope.getString("purchasesPanelViewId");
        var contentVsid = scope.getString("contentViewId");

        var productsPanelEl = getChildViewElement(productsPanelVsid);
        var purchasesPanelEl = getChildViewElement(purchasesPanelVsid);
        var contentViewEl = getChildViewElement(contentVsid);

        // @formatter:off
        return div(Css.ROOT).children(
          renderNavbar(nickName, cartCount),
          div(showError ? Css.ERROR_VISIBLE : Css.HIDDEN).children(
            span(Css.ERROR_ICON),
            span(Css.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
          renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl));
        // @formatter:on
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        // @formatter:off
        return spTheme("dark").cls(Css.NAVBAR).children(
          // Left: exit + greeting
          div(Css.NAV_GROUP).children(
            spActionButton()
              .children(span(Css.EXIT_ICON))
              .on("click", onExit),
            div(Css.GREETING_WRAP).children(
              span(Css.GREETING_LABEL).text("Bem-vindo(a),"),
              span(Css.GREETING_NAME).text(nickName))),
          // Center: logo
          div(Css.NAV_GROUP).children(
            div(Css.LOGO_BOX)
              .children(span(Css.LOGO_ICON)),
            div(Css.LOGO_TEXT_WRAP).children(
              span(Css.LOGO_TITLE).text("Shopping"),
              span(Css.LOGO_SUBTITLE).text("By WeDoCode"))),
          // Right: cart button
          div(Css.NAV_RIGHT).children(
            spActionButton().cls(Css.CART_BTN)
              .on("click", onOpenCart)
              .children(
                span(Css.CART_ICON),
                span(Css.CART_LABEL).text("Carrinho"),
                span(Css.CART_BADGE).text(cartCount))));
        // @formatter:on
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            return slot(contentViewEl)
                    .cls(Css.CONTENT_OVERLAY);
        }

        var productsHide = showingProducts ? "" : "md-show";
        var purchasesHide = showingProducts ? "md-show" : "";

        // @formatter:off
        return div(Css.SPLIT_COL).children(
          renderTabNav(),
          div(Css.SPLIT_ROW).children(
            slot(productsPanelEl).cls(clsx(productsHide, Css.PANEL_SLOT)),
            slot(purchasesPanelEl).cls(clsx("slot-purchases md-grow-0", purchasesHide, Css.PANEL_SLOT))));
        // @formatter:on
    }

    private VNode renderTabNav() {
        // @formatter:off
        return nav(Css.TAB_NAV).children(
          button(showingProducts ? Css.TAB_ACTIVE : Css.TAB_INACTIVE)
            .on("click", onTabProducts)
            .children(
              span(Css.TAB_ICON_PRODUCTS),
              span().text("Produtos"),
              showingProducts ? span(Css.TAB_INDICATOR) : span(Css.HIDDEN)),
          button(showingProducts ? Css.TAB_INACTIVE : Css.TAB_ACTIVE)
            .on("click", onTabHistory)
            .children(
              span(Css.TAB_ICON_HISTORY),
              span().text("Histórico"),
              !showingProducts ? span(Css.TAB_INDICATOR) : span(Css.HIDDEN)));
        // @formatter:on
    }

    private void switchTab(boolean showProducts) {
        // If a content view (product detail, cart, etc.) is showing, navigate back first
        var scope = state();
        String contentVsid = scope.getString("contentViewId");
        if (contentVsid != null && !contentVsid.isEmpty()) {
            historyBack();
        }
        this.showingProducts = showProducts;
        forceUpdate();
    }

    @JSBody(params = {}, script = "history.back();")
    private static native void historyBack();
}
