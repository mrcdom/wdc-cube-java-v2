package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.theme.AppStyles.*;
import static br.com.wdc.shopping.view.teavm.theme.BsColors.*;
import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.theme.BsIcons;
import br.com.wdc.shopping.view.teavm.vdom.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.vdom.VNode;

public class HomeViewVDom extends AbstractVDomView<HomePresenter> {

    private final HomeViewState state;
    private boolean showingProducts = true;

    public HomeViewVDom(HomePresenter presenter) {
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

        return div("d-flex flex-column flex-grow-1").style("flex:1;min-height:0;overflow:hidden").children(
                // App bar
                renderNavbar(nickName, cartCount),

                // Error
                div("alert alert-danger m-2")
                        .style(showError ? "" : "display:none")
                        .text(errorMessage),

                // Content pane
                renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl),

                // Bottom navigation
                renderBottomNav());
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        return nav("navbar navbar-dark px-2 px-sm-3")
                .style(NAVBAR)
                .children(
                        // Left: exit + greeting
                        div("d-flex align-items-center gap-1 gap-sm-2").children(
                                button("")
                                        .style(NAVBAR_BUTTON)
                                        .children(span(BsIcons.POWER))
                                        .on("click", evt -> safeAction("Exit", this.presenter::onExit)),
                                span("d-none d-sm-inline")
                                        .style(NAVBAR_TEXT)
                                        .text("Olá,"),
                                span("d-none d-sm-inline")
                                        .style(NAVBAR_TEXT_BOLD)
                                        .text(nickName)),

                        // Center: logo
                        div("d-flex align-items-center gap-1 gap-sm-2").children(
                                span(BsIcons.CART).style(APP_LOGO_ICON_SM),
                                div("").children(
                                        span("")
                                                .style(APP_LOGO_TEXT_SM)
                                                .text("Shopping"),
                                        div("d-none d-sm-block")
                                                .style(APP_LOGO_SUBTITLE_SM)
                                                .text("by WeDoCode"))),

                        // Right: cart button
                        div("d-flex align-items-center pe-2").children(
                                button("")
                                        .style(CART_BUTTON)
                                        .on("click", evt -> safeAction("Open cart", this.presenter::onOpenCart))
                                        .children(
                                                span(BsIcons.CART),
                                                span("d-none d-sm-inline")
                                                        .style("font-size:0.9rem;color:" + TEXT_ON_PRIMARY)
                                                        .text("Carrinho"),
                                                span("")
                                                        .style(CART_BADGE)
                                                        .text(cartCount))));
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            // Showing a content view (product detail, cart, receipt)
            return slot("flex-grow-1", contentViewEl)
                    .style(CONTENT_PANE);
        }

        // Responsive classes: on mobile, only one panel visible at a time
        // On desktop (md+), both panels always visible side by side
        var productsHide = this.showingProducts ? "" : "d-none d-md-flex";
        var purchasesHide = this.showingProducts ? "d-none d-md-flex" : "";

        return div("flex-grow-1 d-flex flex-column flex-md-row")
                .style(CONTENT_PANE)
                .children(
                        slot("flex-grow-1 h-100 " + productsHide, productsPanelEl)
                                .style("display:flex;flex-direction:column"),
                        slot("h-100 " + purchasesHide, purchasesPanelEl)
                                .style("width:320px;flex-shrink:0;display:flex;flex-direction:column"));
    }

    private VNode renderBottomNav() {
        var prodCls = this.showingProducts
                ? "btn flex-grow-1 rounded-0 py-3 fw-bold text-primary border-0"
                : "btn flex-grow-1 rounded-0 py-3 text-muted border-0";
        var histCls = this.showingProducts
                ? "btn flex-grow-1 rounded-0 py-3 text-muted border-0"
                : "btn flex-grow-1 rounded-0 py-3 fw-bold text-primary border-0";

        // Bottom nav only on mobile (hidden on md+)
        return footer("d-flex d-md-none border-top bg-white flex-shrink-0").children(
                button(prodCls)
                        .children(span(BsIcons.SHOP), span("").text(" Produtos"))
                        .on("click", evt -> switchTab(true)),
                button(histCls)
                        .children(span(BsIcons.CLOCK_HISTORY), span("").text(" Histórico"))
                        .on("click", evt -> switchTab(false)));
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
