package br.com.wdc.shopping.view.teavm.views.home;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView.HomeViewData;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;
import br.com.wdc.shopping.view.teavm.views.AbstractViewTeaVM;

public class HomeView extends AbstractVDomView<HomePresenter> {

    private final HomeSharedView shared;
    private final HomeViewData viewData = new HomeViewData();

    public HomeView(HomePresenter presenter) {
        super("home", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new HomeSharedView();
        this.shared.stateSupplier = this::buildViewData;
        this.shared.requestUpdate = this::update;
        this.shared.onExit = () -> safeAction("Exit", presenter.state.onExit::run);
        this.shared.onOpenCart = () -> safeAction("Open cart", presenter.state.onOpenCart::run);
        this.shared.onNavigateBack = () -> safeAction("Back to home", () -> new HomeKeys(this.app).flip());
        this.shared.onPurchasesPanelVisible = () -> {
            var purchasesPanel = presenter.state.purchasesPanel.get();
            if (purchasesPanel != null && purchasesPanel.view() != null) {
                purchasesPanel.view().update();
            }
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private HomeViewData buildViewData() {
        var state = this.presenter.state;
        viewData.nickName = state.nickName.get() != null ? state.nickName.get() : "";
        viewData.cartCount = String.valueOf(state.cartItemCount.get());
        viewData.errorMessage = state.errorMessage.get();
        viewData.productsPanelEl = state.productsPanel.get() != null && state.productsPanel.get().view() instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.purchasesPanelEl = state.purchasesPanel.get() != null && state.purchasesPanel.get().view() instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.contentViewEl = state.contentView.get() instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        return viewData;
    }
}
