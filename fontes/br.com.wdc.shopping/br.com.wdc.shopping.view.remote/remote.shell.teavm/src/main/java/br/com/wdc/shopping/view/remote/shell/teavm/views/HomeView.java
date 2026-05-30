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
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.vdom.CssComponents;
import br.com.wdc.framework.vdom.CssIcons;
import br.com.wdc.framework.vdom.CssUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

/**
 * Home view: navbar + content panels.
 * State fields: nickName, cartItemCount, productsPanelViewId, purchasesPanelViewId, contentViewId, errorMessage.
 */
public class HomeView extends AbstractRemoteView {

    public static final String VIEW_ID = "473dbdd7a36a";

    private static final int ON_EXIT = 1;
    private static final int ON_OPEN_CART = 2;

    @SuppressWarnings("java:S1214")
    private interface Css {

        String ROOT = clsx(CssUtility.FLEX_COL, CssUtility.FLEX_GROW, CssUtility.FLEX_1, CssUtility.MIN_H_0, CssUtility.OVERFLOW_HIDDEN);

        String ERROR_VISIBLE = CssComponents.ALERT_ERROR;

        String HIDDEN = CssUtility.HIDDEN;

        String ERROR_ICON = clsx(CssIcons.EXCLAMATION_CIRCLE, CssComponents.ALERT_ERROR_ICON);

        String ERROR_TEXT = CssComponents.ALERT_ERROR_TEXT;

        // Navbar
        String NAVBAR = "navbar";

        String NAV_GROUP = "nav-group";

        String EXIT_ICON = clsx(CssIcons.BOX_ARROW_LEFT, CssUtility.TEXT_LG, CssUtility.TEXT_WHITE);

        String GREETING_WRAP = clsx(CssUtility.SM_SHOW, CssUtility.FLEX_COL, CssUtility.LEADING_TIGHT);

        String GREETING_LABEL = clsx(CssUtility.TEXT_XS, CssUtility.TEXT_WHITE_70, CssUtility.FONT_NORMAL);

        String GREETING_NAME = clsx(CssUtility.TEXT_SM, CssUtility.FONT_SEMIBOLD, CssUtility.TEXT_WHITE);

        String LOGO_BOX = "logo-box";

        String LOGO_ICON = clsx(CssIcons.BAG_CHECK, CssUtility.TEXT_XL, CssUtility.TEXT_WHITE);

        String LOGO_TEXT_WRAP = clsx(CssUtility.FLEX_COL, CssUtility.LEADING_TIGHT);

        String LOGO_TITLE = clsx(CssUtility.TEXT_BASE, CssUtility.FONT_BOLD, CssUtility.TEXT_WHITE, CssUtility.TRACKING_TIGHT);

        String LOGO_SUBTITLE = clsx(CssUtility.SM_SHOW, CssUtility.TEXT_XS, CssUtility.TEXT_WHITE_65, CssUtility.FONT_NORMAL, CssUtility.TRACKING_WIDE);

        String NAV_RIGHT = CssUtility.FLEX_ITEMS_CENTER;

        String CART_BTN = CssUtility.RELATIVE;

        String CART_ICON = clsx(CssIcons.BAG, CssUtility.TEXT_XL, CssUtility.TEXT_WHITE);

        String CART_LABEL = clsx(CssUtility.SM_SHOW, CssUtility.TEXT_SM, CssUtility.TEXT_WHITE, CssUtility.FONT_MEDIUM, CssUtility.ML_6);

        String CART_BADGE = "cart-badge";

        // Content pane
        String CONTENT_OVERLAY = clsx(CssUtility.FLEX_COL, CssUtility.FLEX_GROW, CssUtility.OVERFLOW_AUTO, CssUtility.MIN_H_0, CssUtility.BG_DEFAULT);

        String SPLIT_ROW = clsx(CssUtility.MD_ROW, CssUtility.FLEX, CssUtility.FLEX_GROW, CssUtility.OVERFLOW_AUTO, CssUtility.MIN_H_0, CssUtility.BG_DEFAULT);

        String PANEL_SLOT = clsx(CssUtility.FLEX_COL, CssUtility.FLEX_GROW, CssUtility.H_FULL);

        // Tab nav
        String TAB_NAV = clsx(CssUtility.MD_HIDE, "tab-nav");

        String TAB_ACTIVE = "tab-item tab-item--active";

        String TAB_INACTIVE = "tab-item tab-item--inactive";

        String TAB_INDICATOR = "tab-indicator";

        String TAB_ICON_PRODUCTS = clsx(CssIcons.GRID_3X3_GAP, CssUtility.TEXT_BASE);
        String TAB_ICON_HISTORY = clsx(CssIcons.CLOCK_HISTORY, CssUtility.TEXT_BASE);
    }

    private boolean showingProducts = true;

    public HomeView(String vsid) {
        super(vsid);
    }

    @Override
    protected VNode render() {
        var scope = state();
        String nickName = scope.getString("nickName", "");
        String cartCount = String.valueOf(scope.getInt("cartItemCount"));
        String errorMessage = scope.getString("errorMessage");
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        // Child view elements
        String productsPanelVsid = scope.getString("productsPanelViewId");
        String purchasesPanelVsid = scope.getString("purchasesPanelViewId");
        String contentVsid = scope.getString("contentViewId");

        HTMLElement productsPanelEl = getChildViewElement(productsPanelVsid);
        HTMLElement purchasesPanelEl = getChildViewElement(purchasesPanelVsid);
        HTMLElement contentViewEl = getChildViewElement(contentVsid);

        // @formatter:off
        return div(Css.ROOT).children(
          renderNavbar(nickName, cartCount),
          renderTabNav(),
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
              .on("click", evt -> submit(ON_EXIT)),
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
              .on("click", evt -> submit(ON_OPEN_CART))
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
        return div(Css.SPLIT_ROW).children(
          slot(productsPanelEl).cls(clsx(productsHide, Css.PANEL_SLOT)),
          slot(purchasesPanelEl).cls(clsx("slot-purchases md-grow-0", purchasesHide, Css.PANEL_SLOT)));
        // @formatter:on
    }

    private VNode renderTabNav() {
        // @formatter:off
        return nav(Css.TAB_NAV).children(
          button(showingProducts ? Css.TAB_ACTIVE : Css.TAB_INACTIVE)
            .on("click", evt -> switchTab(true))
            .children(
              span(Css.TAB_ICON_PRODUCTS),
              span().text("Produtos"),
              showingProducts ? span(Css.TAB_INDICATOR) : span(Css.HIDDEN)),
          button(showingProducts ? Css.TAB_INACTIVE : Css.TAB_ACTIVE)
            .on("click", evt -> switchTab(false))
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
