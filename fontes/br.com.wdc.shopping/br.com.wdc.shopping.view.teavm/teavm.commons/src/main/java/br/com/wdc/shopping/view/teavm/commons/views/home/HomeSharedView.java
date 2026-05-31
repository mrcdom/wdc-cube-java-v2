package br.com.wdc.shopping.view.teavm.commons.views.home;

import static br.com.wdc.shopping.view.teavm.commons.Swc.spActionButton;
import static br.com.wdc.shopping.view.teavm.commons.Swc.spTheme;
import static br.com.wdc.shopping.view.teavm.commons.VNode.button;
import static br.com.wdc.shopping.view.teavm.commons.VNode.clsx;
import static br.com.wdc.shopping.view.teavm.commons.VNode.div;
import static br.com.wdc.shopping.view.teavm.commons.VNode.nav;
import static br.com.wdc.shopping.view.teavm.commons.VNode.slot;
import static br.com.wdc.shopping.view.teavm.commons.VNode.span;

import java.util.function.Supplier;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.view.teavm.commons.SelComponents;
import br.com.wdc.shopping.view.teavm.commons.SelIcons;
import br.com.wdc.shopping.view.teavm.commons.SelUtility;
import br.com.wdc.shopping.view.teavm.commons.SharedVDomView;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Shared Home view: navbar + content panels with tab switching.
 */
public class HomeSharedView extends SharedVDomView {

    @SuppressWarnings({"java:S1214", "static-access"})
    private interface Sel {
        SelUtility u = SelUtility.INSTANCE;
        SelComponents c = SelComponents.INSTANCE;
        SelIcons icon = SelIcons.INSTANCE;

        String HIDDEN = u.HIDDEN;
        String ROOT = clsx(u.FLEX_COL, u.FLEX_GROW, u.FLEX_1, u.MIN_H_0, u.OVERFLOW_HIDDEN);
        String ERROR_VISIBLE = c.ALERT_ERROR;
        String ERROR_ICON = clsx(icon.EXCLAMATION_CIRCLE, c.ALERT_ERROR_ICON);
        String ERROR_TEXT = c.ALERT_ERROR_TEXT;

        // Navbar
        String NAVBAR = c.NAVBAR;
        String NAV_GROUP = c.NAV_GROUP;
        String NAV_RIGHT = u.FLEX_ITEMS_CENTER;
        String EXIT_ICON = clsx(icon.BOX_ARROW_LEFT, u.TEXT_LG, u.TEXT_WHITE);
        String GREETING_WRAP = clsx(u.SM_SHOW, u.FLEX_COL, u.LEADING_TIGHT);
        String GREETING_LABEL = clsx(u.TEXT_XS, u.TEXT_WHITE_70, u.FONT_NORMAL);
        String GREETING_NAME = clsx(u.TEXT_SM, u.FONT_SEMIBOLD, u.TEXT_WHITE);
        String LOGO_BOX = c.LOGO_BOX;
        String LOGO_ICON = clsx(icon.BAG_CHECK, u.TEXT_XL, u.TEXT_WHITE);
        String LOGO_TEXT_WRAP = clsx(u.FLEX_COL, u.LEADING_TIGHT);
        String LOGO_TITLE = clsx(u.TEXT_BASE, u.FONT_BOLD, u.TEXT_WHITE, u.TRACKING_TIGHT);
        String LOGO_SUBTITLE = clsx(u.SM_SHOW, u.TEXT_XS, u.TEXT_WHITE_65, u.FONT_NORMAL, u.TRACKING_WIDE);
        String CART_BTN = u.RELATIVE;
        String CART_ICON = clsx(icon.BAG, u.TEXT_XL, u.TEXT_WHITE);
        String CART_LABEL = clsx(u.SM_SHOW, u.TEXT_SM, u.TEXT_WHITE, u.FONT_MEDIUM, u.ML_6);
        String CART_BADGE = c.CART_BADGE;

        // Content pane
        String CONTENT_OVERLAY = clsx(u.FLEX_COL, u.FLEX_GROW, u.OVERFLOW_AUTO, u.MIN_H_0, u.BG_DEFAULT);
        String SPLIT_COL = clsx(u.FLEX_COL, u.FLEX_GROW, u.MIN_H_0, u.OVERFLOW_HIDDEN);
        String SPLIT_ROW = clsx(u.MD_ROW, u.FLEX, u.FLEX_GROW, u.OVERFLOW_AUTO, u.MIN_H_0, u.BG_DEFAULT);
        String PANEL_SLOT = clsx(u.FLEX_COL, u.FLEX_GROW, u.H_FULL);

        // Tab nav
        String TAB_NAV = clsx(u.MD_HIDE, c.TAB_NAV);
        String TAB_ACTIVE = c.TAB_ITEM_ACTIVE;
        String TAB_INACTIVE = c.TAB_ITEM_INACTIVE;
        String TAB_INDICATOR = c.TAB_INDICATOR;
        String TAB_ICON_PRODUCTS = clsx(icon.GRID_3X3_GAP, u.TEXT_BASE);
        String TAB_ICON_HISTORY = clsx(icon.CLOCK_HISTORY, u.TEXT_BASE);
    }

    /**
     * Data provided by the adapter for each render cycle.
     */
    public static class HomeViewData {
        public String nickName = "";
        public String cartCount = "0";
        public String errorMessage;
        public HTMLElement productsPanelEl;
        public HTMLElement purchasesPanelEl;
        public HTMLElement contentViewEl;
    }

    // -- External bindings --

    public Supplier<HomeViewData> stateSupplier;
    public Runnable onExit;
    public Runnable onOpenCart;
    /** Called when tab switches while content view is showing (navigate back). */
    public Runnable onNavigateBack;
    /** Called when purchases panel tab becomes visible. */
    public Runnable onPurchasesPanelVisible;
    public Runnable requestUpdate;

    // -- Local state --

    private boolean showingProducts = true;

    // -- Stable event listeners --

    private final EventListener<Event> exitListener = evt -> { if (onExit != null) onExit.run(); };
    private final EventListener<Event> openCartListener = evt -> { if (onOpenCart != null) onOpenCart.run(); };
    private final EventListener<Event> tabProductsListener = evt -> switchTab(true);
    private final EventListener<Event> tabHistoryListener = evt -> switchTab(false);

    // -- Render --

    @Override
    public VNode render() {
        var data = stateSupplier.get();
        var errorMessage = data.errorMessage;
        boolean showError = errorMessage != null && !errorMessage.isEmpty();

        // @formatter:off
        return div(Sel.ROOT).children(
          renderNavbar(data.nickName, data.cartCount),
          div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
            span(Sel.ERROR_ICON),
            span(Sel.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
          renderContentPane(data.productsPanelEl, data.purchasesPanelEl, data.contentViewEl));
        // @formatter:on
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        // @formatter:off
        return spTheme("dark").cls(Sel.NAVBAR).children(
          // Left: exit + greeting
          div(Sel.NAV_GROUP).children(
            spActionButton()
              .children(span(Sel.EXIT_ICON))
              .on("click", exitListener),
            div(Sel.GREETING_WRAP).children(
              span(Sel.GREETING_LABEL).text("Bem-vindo(a),"),
              span(Sel.GREETING_NAME).text(nickName))),
          // Center: logo
          div(Sel.NAV_GROUP).children(
            div(Sel.LOGO_BOX)
              .children(span(Sel.LOGO_ICON)),
            div(Sel.LOGO_TEXT_WRAP).children(
              span(Sel.LOGO_TITLE).text("Shopping"),
              span(Sel.LOGO_SUBTITLE).text("By WeDoCode"))),
          // Right: cart button
          div(Sel.NAV_RIGHT).children(
            spActionButton().cls(Sel.CART_BTN)
              .on("click", openCartListener)
              .children(
                span(Sel.CART_ICON),
                span(Sel.CART_LABEL).text("Carrinho"),
                span(Sel.CART_BADGE).text(cartCount))));
        // @formatter:on
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            return slot(contentViewEl).cls(Sel.CONTENT_OVERLAY);
        }

        var productsHide = this.showingProducts ? "" : "md-show";
        var purchasesHide = this.showingProducts ? "md-show" : "";

        // @formatter:off
        return div(Sel.SPLIT_COL).children(
          renderTabNav(),
          div(Sel.SPLIT_ROW).children(
            slot(productsPanelEl).cls(clsx(productsHide, Sel.PANEL_SLOT)),
            slot(purchasesPanelEl).cls(clsx("slot-purchases md-grow-0", purchasesHide, Sel.PANEL_SLOT))));
        // @formatter:on
    }

    private VNode renderTabNav() {
        // @formatter:off
        return nav(Sel.TAB_NAV).children(
          button(this.showingProducts ? Sel.TAB_ACTIVE : Sel.TAB_INACTIVE)
            .on("click", tabProductsListener)
            .children(
              span(Sel.TAB_ICON_PRODUCTS),
              span().text("Produtos"),
              this.showingProducts ? span(Sel.TAB_INDICATOR) : span(Sel.HIDDEN)),
          button(this.showingProducts ? Sel.TAB_INACTIVE : Sel.TAB_ACTIVE)
            .on("click", tabHistoryListener)
            .children(
              span(Sel.TAB_ICON_HISTORY),
              span().text("Histórico"),
              !this.showingProducts ? span(Sel.TAB_INDICATOR) : span(Sel.HIDDEN)));
        // @formatter:on
    }

    private void switchTab(boolean showProducts) {
        var data = stateSupplier.get();
        if (data.contentViewEl != null) {
            if (onNavigateBack != null) onNavigateBack.run();
        }
        this.showingProducts = showProducts;
        if (requestUpdate != null) requestUpdate.run();
        if (!showProducts && onPurchasesPanelVisible != null) {
            onPurchasesPanelVisible.run();
        }
    }
}
