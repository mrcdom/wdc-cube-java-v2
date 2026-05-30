package br.com.wdc.shopping.view.remote.shell.teavm.views;

import static br.com.wdc.framework.vdom.StyleBuilder.css;
import static br.com.wdc.framework.vdom.Swc.spActionButton;
import static br.com.wdc.framework.vdom.Swc.spTheme;
import static br.com.wdc.framework.vdom.VNode.button;
import static br.com.wdc.framework.vdom.VNode.div;
import static br.com.wdc.framework.vdom.VNode.nav;
import static br.com.wdc.framework.vdom.VNode.slot;
import static br.com.wdc.framework.vdom.VNode.span;

import org.teavm.jso.JSBody;
import org.teavm.jso.dom.html.HTMLElement;

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
    private interface Styles {

        String ROOT = css()
                .flexCol()
                .flexGrow(1)
                .flex("1")
                .minHeight("0")
                .overflowHidden()
                .build();

        String ERROR_VISIBLE = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px")
                .padding("12px 16px")
                .margin("8px 16px")
                .background("#fef2f2")
                .border("1px solid #fecaca")
                .borderRadius("var(--app-radius-sm)")
                .build();

        String HIDDEN = css()
                .displayNone()
                .build();

        String ERROR_ICON = css()
                .color("#dc2626")
                .fontSize("1rem")
                .flexShrink(0).build();

        String ERROR_TEXT = css()
                .fontSize("0.85rem")
                .color("#991b1b")
                .fontWeight("500")
                .build();

        // Navbar
        String NAVBAR = css()
                .displayFlex()
                .alignItems("center")
                .justifyContent("space-between")
                .padding("10px 16px")
                .background("linear-gradient(135deg, #0d66d0 0%, #1a8cff 100%)")
                .flexShrink(0)
                .boxShadow("0 2px 8px rgba(13,102,208,0.3)")
                .build();

        String NAV_GROUP = css()
                .displayFlex()
                .alignItems("center")
                .gap("10px").build();

        String EXIT_ICON = css()
                .fontSize("1.1rem")
                .color("#fff")
                .build();

        String GREETING_WRAP = css()
                .flexCol()
                .lineHeight("1.2")
                .build();

        String GREETING_LABEL = css()
                .fontSize("0.7rem")
                .color("rgba(255,255,255,0.7)")
                .fontWeight("400")
                .build();

        String GREETING_NAME = css()
                .fontSize("0.85rem")
                .fontWeight("600")
                .color("#fff")
                .build();

        String LOGO_BOX = css()
                .width("36px")
                .height("36px")
                .background("rgba(255,255,255,0.15)")
                .borderRadius("10px")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .prop("backdrop-filter", "blur(4px)")
                .build();

        String LOGO_ICON = css()
                .fontSize("1.2rem")
                .color("#fff")
                .build();

        String LOGO_TEXT_WRAP = css()
                .flexCol()
                .lineHeight("1.2")
                .build();

        String LOGO_TITLE = css()
                .fontSize("1rem")
                .fontWeight("700")
                .color("#fff")
                .prop("letter-spacing", "-0.3px")
                .build();

        String LOGO_SUBTITLE = css()
                .fontSize("0.6rem")
                .color("rgba(255,255,255,0.65)")
                .fontWeight("400")
                .prop("letter-spacing", "0.5px")
                .prop("text-transform", "none")
                .build();

        String NAV_RIGHT = css()
                .displayFlex()
                .alignItems("center")
                .build();

        String CART_BTN = css()
                .position("relative")
                .build();

        String CART_ICON = css()
                .fontSize("1.2rem")
                .color("#fff")
                .build();

        String CART_LABEL = css()
                .fontSize("0.85rem")
                .color("#fff")
                .fontWeight("500")
                .marginLeft("6px")
                .build();

        String CART_BADGE = css()
                .background("#fff")
                .color("var(--app-accent)")
                .fontSize("0.65rem")
                .fontWeight("700")
                .padding("2px 6px")
                .borderRadius("10px")
                .minWidth("18px")
                .textAlign("center")
                .marginLeft("8px")
                .boxShadow("0 2px 4px rgba(0,0,0,0.15)")
                .build();

        // Content pane
        String CONTENT_OVERLAY = css()
                .flexCol()
                .flexGrow(1)
                .overflow("auto")
                .minHeight("0")
                .background("var(--app-bg)")
                .build();

        String SPLIT_ROW = css()
                .displayFlex()
                .flexGrow(1)
                .overflow("auto")
                .minHeight("0")
                .background("var(--app-bg)")
                .build();

        String PANEL_SLOT = css()
                .flexCol()
                .flexGrow(1)
                .height("100%")
                .build();

        // Tab nav
        String TAB_NAV = css()
                .displayFlex()
                .background("var(--app-surface)")
                .flexShrink(0)
                .boxShadow("0 1px 0 var(--app-border)")
                .build();

        String TAB_ACTIVE = css()
                .flex("1")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .gap("6px")
                .padding("12px 0")
                .fontSize("0.8rem")
                .fontWeight("600")
                .cursor("pointer")
                .border("none")
                .background("none")
                .position("relative")
                .color("var(--app-accent)")
                .transition("color 0.2s")
                .build();

        String TAB_INACTIVE = css()
                .flex("1")
                .displayFlex()
                .alignItems("center")
                .justifyContent("center")
                .gap("6px")
                .padding("12px 0")
                .fontSize("0.8rem")
                .fontWeight("500")
                .cursor("pointer")
                .border("none")
                .background("none")
                .position("relative")
                .color("var(--app-text-secondary)")
                .transition("color 0.2s")
                .build();

        String TAB_INDICATOR = css()
                .position("absolute")
                .bottom("0")
                .left("16px")
                .right("16px")
                .height("2.5px")
                .background("var(--app-accent)")
                .borderRadius("2px")
                .build();

        String TAB_ICON = css()
                .fontSize("1rem")
                .build();
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
        return div().style(Styles.ROOT).children(
          renderNavbar(nickName, cartCount),
          renderTabNav(),
          div().style(showError ? Styles.ERROR_VISIBLE : Styles.HIDDEN).children(
            span().cls("bi bi-exclamation-circle").style(Styles.ERROR_ICON),
            span().style(Styles.ERROR_TEXT).text(errorMessage != null ? errorMessage : "")),
          renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl));
        // @formatter:on
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        // @formatter:off
        return spTheme("dark").style(Styles.NAVBAR).children(
          // Left: exit + greeting
          div().style(Styles.NAV_GROUP).children(
            spActionButton()
              .children(span().cls("bi bi-box-arrow-left").style(Styles.EXIT_ICON))
              .on("click", evt -> submit(ON_EXIT)),
            div().cls("sm-show").style(Styles.GREETING_WRAP).children(
              span().style(Styles.GREETING_LABEL).text("Bem-vindo(a),"),
              span().style(Styles.GREETING_NAME).text(nickName))),
          // Center: logo
          div().style(Styles.NAV_GROUP).children(
            div().style(Styles.LOGO_BOX).children(
              span().cls("bi bi-bag-check").style(Styles.LOGO_ICON)),
            div().style(Styles.LOGO_TEXT_WRAP).children(
              span().style(Styles.LOGO_TITLE).text("Shopping"),
              span().cls("sm-show").style(Styles.LOGO_SUBTITLE).text("By WeDoCode"))),
          // Right: cart button
          div().style(Styles.NAV_RIGHT).children(
            spActionButton().style(Styles.CART_BTN)
              .on("click", evt -> submit(ON_OPEN_CART))
              .children(
                span().cls("bi bi-bag").style(Styles.CART_ICON),
                span().cls("sm-show").style(Styles.CART_LABEL).text("Carrinho"),
                span().style(Styles.CART_BADGE).text(cartCount))));
        // @formatter:on
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            return slot(contentViewEl)
                    .style(Styles.CONTENT_OVERLAY);
        }

        var productsHide = showingProducts ? "" : "md-show";
        var purchasesHide = showingProducts ? "md-show" : "";

        // @formatter:off
        return div().cls("md-row").style(Styles.SPLIT_ROW).children(
          slot(productsPanelEl).cls(productsHide).style(Styles.PANEL_SLOT),
          slot(purchasesPanelEl).cls("slot-purchases md-grow-0 " + purchasesHide).style(Styles.PANEL_SLOT));
        // @formatter:on
    }

    private VNode renderTabNav() {
        // @formatter:off
        return nav().cls("md-hide").style(Styles.TAB_NAV).children(
          button().style(showingProducts ? Styles.TAB_ACTIVE : Styles.TAB_INACTIVE)
            .children(
              span().cls("bi bi-grid-3x3-gap").style(Styles.TAB_ICON),
              span().text("Produtos"),
              showingProducts ? span().style(Styles.TAB_INDICATOR) : span().style(Styles.HIDDEN))
            .on("click", evt -> switchTab(true)),
          button().style(showingProducts ? Styles.TAB_INACTIVE : Styles.TAB_ACTIVE)
            .children(
              span().cls("bi bi-clock-history").style(Styles.TAB_ICON),
              span().text("Histórico"),
              !showingProducts ? span().style(Styles.TAB_INDICATOR) : span().style(Styles.HIDDEN))
            .on("click", evt -> switchTab(false)));
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
