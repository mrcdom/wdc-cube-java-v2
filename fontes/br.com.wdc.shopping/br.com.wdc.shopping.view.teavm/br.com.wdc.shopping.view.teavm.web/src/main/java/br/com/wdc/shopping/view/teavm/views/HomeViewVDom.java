package br.com.wdc.shopping.view.teavm.views;

import static br.com.wdc.shopping.view.teavm.vdom.VNode.*;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.shopping.presentation.presenter.Routes;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.teavm.AbstractViewTeaVM;
import br.com.wdc.shopping.view.teavm.ShoppingTeaVMApplication;
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

        // @formatter:off
        return div("")
                .style("display:flex;flex-direction:column;flex-grow:1;flex:1;min-height:0;overflow:hidden")
                .children(
                    renderNavbar(nickName, cartCount),
                    renderTabNav(),
                    div("")
                            .style(showError ? "display:flex;align-items:center;gap:10px;padding:12px 16px;margin:8px 16px;background:#fef2f2;border:1px solid #fecaca;border-radius:var(--app-radius-sm)" : "display:none")
                            .children(
                                    span("bi bi-exclamation-circle").style("color:#dc2626;font-size:1rem;flex-shrink:0"),
                                    span("").style("font-size:0.85rem;color:#991b1b;font-weight:500").text(errorMessage)),
                    renderContentPane(productsPanelEl, purchasesPanelEl, contentViewEl));
        // @formatter:on
    }

    private VNode renderNavbar(String nickName, String cartCount) {
        // @formatter:off
        return el("sp-theme")
                .attr("color", "dark")
                .attr("scale", "medium")
                .style("display:flex;align-items:center;justify-content:space-between;padding:10px 16px;background:linear-gradient(135deg, #0d66d0 0%, #1a8cff 100%);flex-shrink:0;box-shadow:0 2px 8px rgba(13,102,208,0.3)")
                .children(
                        // Left: exit + greeting
                        div("")
                                .style("display:flex;align-items:center;gap:10px")
                                .children(
                                        el("sp-action-button")
                                                .boolAttr("quiet", true)
                                                .children(span("bi bi-box-arrow-left").style("font-size:1.1rem;color:#fff"))
                                                .on("click", evt -> safeAction("Exit", this.presenter::onExit)),
                                        div("sm-show")
                                                .style("display:flex;flex-direction:column;line-height:1.2")
                                                .children(
                                                        span("")
                                                                .style("font-size:0.7rem;color:rgba(255,255,255,0.7);font-weight:400")
                                                                .text("Bem-vindo(a),"),
                                                        span("")
                                                                .style("font-size:0.85rem;font-weight:600;color:#fff")
                                                                .text(nickName))),

                        // Center: logo
                        div("")
                                .style("display:flex;align-items:center;gap:10px")
                                .children(
                                        div("")
                                                .style("width:36px;height:36px;background:rgba(255,255,255,0.15);border-radius:10px;display:flex;align-items:center;justify-content:center;backdrop-filter:blur(4px)")
                                                .children(span("bi bi-bag-check").style("font-size:1.2rem;color:#fff")),
                                        div("").style("display:flex;flex-direction:column;line-height:1.2").children(
                                                span("")
                                                        .style("font-size:1rem;font-weight:700;color:#fff;letter-spacing:-0.3px")
                                                        .text("Shopping"),
                                                span("sm-show")
                                                        .style("font-size:0.6rem;color:rgba(255,255,255,0.65);font-weight:400;letter-spacing:0.5px;text-transform:none")
                                                        .text("By WeDoCode"))),

                        // Right: cart button
                        div("")
                                .style("display:flex;align-items:center")
                                .children(
                                        el("sp-action-button")
                                                .boolAttr("quiet", true)
                                                .style("position:relative")
                                                .on("click", evt -> safeAction("Open cart", this.presenter::onOpenCart))
                                                .children(
                                                        span("bi bi-bag").style("font-size:1.2rem;color:#fff"),
                                                        span("sm-show")
                                                                .style("font-size:0.85rem;color:#fff;font-weight:500;margin-left:6px")
                                                                .text("Carrinho"),
                                                        span("")
                                                                .style("background:#fff;color:var(--app-accent);font-size:0.65rem;font-weight:700;padding:2px 6px;border-radius:10px;min-width:18px;text-align:center;margin-left:8px;box-shadow:0 2px 4px rgba(0,0,0,0.15)")
                                                                .text(cartCount))));
        // @formatter:on
    }

    private VNode renderContentPane(HTMLElement productsPanelEl, HTMLElement purchasesPanelEl,
            HTMLElement contentViewEl) {
        if (contentViewEl != null) {
            return slot("", contentViewEl)
                    .style("display:flex;flex-direction:column;flex-grow:1;overflow:auto;min-height:0;background:var(--app-bg)");
        }

        var productsHide = this.showingProducts ? "" : "md-show";
        var purchasesHide = this.showingProducts ? "md-show" : "";

        // @formatter:off
        return div("md-row")
                .style("display:flex;flex-grow:1;overflow:auto;min-height:0;background:var(--app-bg)")
                .children(
                        slot("" + productsHide, productsPanelEl)
                                .style("display:flex;flex-direction:column;flex-grow:1;height:100%"),
                        slot("slot-purchases md-grow-0 " + purchasesHide, purchasesPanelEl)
                                .style("display:flex;flex-direction:column;flex-grow:1;height:100%"));
        // @formatter:on
    }

    private VNode renderTabNav() {
        var activeTabStyle = "flex:1;display:flex;align-items:center;justify-content:center;gap:6px;padding:12px 0;font-size:0.8rem;font-weight:600;cursor:pointer;border:none;background:none;position:relative;color:var(--app-accent);transition:color 0.2s";
        var inactiveTabStyle = "flex:1;display:flex;align-items:center;justify-content:center;gap:6px;padding:12px 0;font-size:0.8rem;font-weight:500;cursor:pointer;border:none;background:none;position:relative;color:var(--app-text-secondary);transition:color 0.2s";
        var indicatorStyle = "position:absolute;bottom:0;left:16px;right:16px;height:2.5px;background:var(--app-accent);border-radius:2px";

        // Tab nav only on mobile (hidden on md+)
        // @formatter:off
        return nav("md-hide")
                .style("display:flex;background:var(--app-surface);flex-shrink:0;box-shadow:0 1px 0 var(--app-border)")
                .children(
                        button("")
                                .style(this.showingProducts ? activeTabStyle : inactiveTabStyle)
                                .children(
                                        span("bi bi-grid-3x3-gap").style("font-size:1rem"),
                                        span("").text("Produtos"),
                                        this.showingProducts ? span("").style(indicatorStyle) : span("").style("display:none"))
                                .on("click", evt -> switchTab(true)),
                        button("")
                                .style(this.showingProducts ? inactiveTabStyle : activeTabStyle)
                                .children(
                                        span("bi bi-clock-history").style("font-size:1rem"),
                                        span("").text("Histórico"),
                                        !this.showingProducts ? span("").style(indicatorStyle) : span("").style("display:none"))
                                .on("click", evt -> switchTab(false)));
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
