package br.com.wdc.shopping.view.teavm.views.home;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeScope;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView.HomeViewData;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;

public class HomeView extends AbstractVDomView<HomeScope> {

    private final HomeSharedView shared;
    private final HomeViewData viewData = new HomeViewData();

    public HomeView(ShoppingTeaVMApplication app, HomeScope state) {
        super("home", app, state);

        this.shared = new HomeSharedView();
        this.shared.stateSupplier = this::buildViewData;
        this.shared.requestUpdate = this::update;
        this.shared.onExit = () -> safeAction("Exit", state.onExit::run);
        this.shared.onOpenCart = () -> safeAction("Open cart", state.onOpenCart::run);
        this.shared.onNavigateBack = () -> safeAction("Back to home", () -> new HomeKeys(this.app).flip());
        this.shared.onPurchasesPanelVisible = () -> {
            var purchasesPanel = this.app.viewOf(state.purchasesPanel.get());
            if (purchasesPanel != null) {
                purchasesPanel.update();
            }
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private HomeViewData buildViewData() {
        var state = this.presenter;
        viewData.nickName = state.nickName.get() != null ? state.nickName.get() : "";
        viewData.cartCount = String.valueOf(state.cartItemCount.get());
        viewData.errorMessage = state.errorMessage.get();
        viewData.productsPanelEl = this.app.viewOf(state.productsPanel.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.purchasesPanelEl = this.app.viewOf(state.purchasesPanel.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.contentViewEl = this.app.viewOf(state.content.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        return viewData;
    }
}
