package br.com.wdc.shopping.view.teavm.views;

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

    // Estado local para erro (one-shot)
    private boolean showError;
    private String errorMessage = "";

    public HomeViewVDom(HomePresenter presenter) {
        super("home", (ShoppingTeaVMApplication) presenter.app, presenter);
        this.state = presenter.state;
        this.element.getClassList().add("d-flex", "flex-column", "flex-grow-1");
        this.element.setAttribute("style", "min-height:0");
    }

    @Override
    protected VNode render() {
        // Consumir erro one-shot
        if (this.state.errorCode != 0) {
            this.showError = true;
            this.errorMessage = this.state.errorMessage;
            this.state.errorCode = 0;
            this.state.errorMessage = null;
        } else {
            this.showError = false;
            this.errorMessage = "";
        }

        var nickName = this.state.nickName != null ? this.state.nickName : "";
        var cartCount = String.valueOf(this.state.cartItemCount);

        // Elementos hospedados
        var productsPanelEl = this.state.productsPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        var purchasesPanelEl = this.state.purchasesPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        var contentViewEl = this.state.contentView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;

        return div("d-flex flex-column flex-grow-1").style("min-height:0").children(
                // App bar
                renderNavbar(nickName, cartCount),

                // Error
                div("alert alert-danger m-2")
                        .style(this.showError ? "" : "display:none")
                        .text(this.errorMessage),

                // Content pane
                renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl),

                // Bottom navigation
                renderBottomNav());
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        return nav("navbar navbar-dark px-2 px-sm-3")
                .style("background-color:#1976d2;min-height:56px;box-shadow:0 2px 4px rgba(0,0,0,0.1);"
                        + "display:flex;flex-wrap:nowrap;align-items:center;justify-content:space-between;"
                        + "padding-top:env(safe-area-inset-top, 0px)")
                .children(
                        // Left: exit + greeting
                        div("d-flex align-items-center gap-1 gap-sm-2").children(
                                button("")
                                        .style("background:none;border:none;color:rgba(255,255,255,0.9);font-size:1.25rem;"
                                                + "cursor:pointer;padding:4px 6px;display:flex;align-items:center")
                                        .children(span(BsIcons.POWER))
                                        .on("click", evt -> safeAction("Exit", this.presenter::onExit)),
                                span("d-none d-sm-inline")
                                        .style("color:rgba(255,255,255,0.85);font-size:0.9rem")
                                        .text("Olá,"),
                                span("d-none d-sm-inline")
                                        .style("color:#fff;font-weight:600;font-size:0.9rem")
                                        .text(nickName)),

                        // Center: logo
                        div("d-flex align-items-center gap-1 gap-sm-2").children(
                                span("bi bi-cart3").style("color:#ff9800;font-size:1.25rem"),
                                div("").children(
                                        span("")
                                                .style("color:#fff;font-size:1.1rem;font-weight:700;letter-spacing:0.5px;display:block;line-height:1.2")
                                                .text("Shopping"),
                                        div("d-none d-sm-block")
                                                .style("color:rgba(255,255,255,0.55);font-size:0.6rem;letter-spacing:0.3px")
                                                .text("by WeDoCode"))),

                        // Right: cart button
                        div("d-flex align-items-center pe-2").children(
                                button("")
                                        .style("background:none;border:none;color:rgba(255,255,255,0.9);cursor:pointer;"
                                                + "padding:4px 8px;display:flex;align-items:center;gap:6px;position:relative")
                                        .on("click", evt -> safeAction("Open cart", this.presenter::onOpenCart))
                                        .children(
                                                span(BsIcons.CART),
                                                span("d-none d-sm-inline")
                                                        .style("font-size:0.9rem;color:#fff")
                                                        .text("Carrinho"),
                                                span("")
                                                        .style("position:absolute;top:-2px;right:-6px;font-size:10px;min-width:18px;"
                                                                + "text-align:center;background-color:#ff9800;color:white;border-radius:50%;padding:2px 5px")
                                                        .text(cartCount))));
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            // Showing a content view (product detail, cart, receipt)
            return slot("flex-grow-1 overflow-auto", contentViewEl)
                    .style("background-color:#ededed;min-height:0");
        }
        // Default: products/purchases tabs
        return div("flex-grow-1 overflow-auto")
                .style("background-color:#ededed;min-height:0")
                .children(
                        div("h-100").children(
                                slot("h-100", productsPanelEl)
                                        .style(this.showingProducts ? "" : "display:none"),
                                slot("h-100", purchasesPanelEl)
                                        .style(this.showingProducts ? "display:none" : "")));
    }

    private VNode renderBottomNav() {
        var prodCls = this.showingProducts
                ? "btn flex-grow-1 rounded-0 py-3 fw-bold text-primary border-0"
                : "btn flex-grow-1 rounded-0 py-3 text-muted border-0";
        var histCls = this.showingProducts
                ? "btn flex-grow-1 rounded-0 py-3 text-muted border-0"
                : "btn flex-grow-1 rounded-0 py-3 fw-bold text-primary border-0";

        return footer("d-flex border-top bg-white flex-shrink-0").children(
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
