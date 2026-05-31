package br.com.wdc.shopping.view.teavm.views.home;

import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spTheme;
import static br.com.wdc.framework.vdom.VNode.button;
import static br.com.wdc.framework.vdom.VNode.clsx;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.nav;
import static br.com.wdc.framework.vdom.VNode.slot;
import static br.com.wdc.framework.vdom.VNode.span;

import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;
import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.vdom.SelComponents;
import br.com.wdc.framework.vdom.SelIcons;
import br.com.wdc.framework.vdom.SelUtility;
import br.com.wdc.framework.vdom.VNode;
import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;

public class HomeView extends AbstractVDomView<HomePresenter> {

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

    private final HomeViewState state;
    private boolean showingProducts = true;

    // Stable event listeners
    private final EventListener<Event> onExit = evt -> safeAction("Exit", this.presenter::onExit);
    private final EventListener<Event> onOpenCart = evt -> safeAction("Open cart", this.presenter::onOpenCart);
    private final EventListener<Event> onTabProducts = evt -> switchTab(true);
    private final EventListener<Event> onTabHistory = evt -> switchTab(false);

    public HomeView(HomePresenter presenter) {
        super("home", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        final boolean showError;
        final String errorMessage;
        if (this.state.errorCode != 0) {
            showError = true;
            errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            showError = false;
            errorMessage = "";
        }

        var nickName = this.state.nickName != null ? this.state.nickName : "";
        var cartCount = String.valueOf(this.state.cartItemCount);

        // Elementos hospedados
        var productsPanelEl = this.state.productsPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        var purchasesPanelEl = this.state.purchasesPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        var contentViewEl = this.state.contentView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;

        // @formatter:off
        return div(Sel.ROOT).children(
          renderNavbar(nickName, cartCount),
          div(showError ? Sel.ERROR_VISIBLE : Sel.HIDDEN).children(
            span(Sel.ERROR_ICON),
            span(Sel.ERROR_TEXT).text(errorMessage)),
          renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl));
        // @formatter:on
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        // @formatter:off
        return spTheme("dark").cls(Sel.NAVBAR).children(
          // Left: exit + greeting
          div(Sel.NAV_GROUP).children(
            spActionButton()
              .children(span(Sel.EXIT_ICON))
              .on("click", onExit),
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
              .on("click", onOpenCart)
              .children(
                span(Sel.CART_ICON),
                span(Sel.CART_LABEL).text("Carrinho"),
                span(Sel.CART_BADGE).text(cartCount))));
        // @formatter:on
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            return slot(contentViewEl)
                    .cls(Sel.CONTENT_OVERLAY);
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
            .on("click", onTabProducts)
            .children(
              span(Sel.TAB_ICON_PRODUCTS),
              span().text("Produtos"),
              this.showingProducts ? span(Sel.TAB_INDICATOR) : span(Sel.HIDDEN)),
          button(this.showingProducts ? Sel.TAB_INACTIVE : Sel.TAB_ACTIVE)
            .on("click", onTabHistory)
            .children(
              span(Sel.TAB_ICON_HISTORY),
              span().text("Histórico"),
              !this.showingProducts ? span(Sel.TAB_INDICATOR) : span(Sel.HIDDEN)));
        // @formatter:on
    }

    private void switchTab(boolean showProducts) {
        if (this.state.contentView != null) {
            safeAction("Back to home", () -> Routes.home(this.app));
        }
        this.showingProducts = showProducts;
        update();
        // Trigger re-measurement when the purchases panel becomes visible
        if (!showProducts && this.state.purchasesPanelView != null) {
            this.state.purchasesPanelView.update();
        }
    }
}
