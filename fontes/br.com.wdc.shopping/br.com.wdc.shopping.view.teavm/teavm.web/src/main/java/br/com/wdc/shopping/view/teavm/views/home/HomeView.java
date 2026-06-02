package br.com.wdc.shopping.view.teavm.views.home;

import br.com.wdc.shopping.presentation.presenter.Routes;
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
        this.shared.onExit = () -> safeAction("Exit", presenter::onExit);
        this.shared.onOpenCart = () -> safeAction("Open cart", presenter::onOpenCart);
        this.shared.onNavigateBack = () -> safeAction("Back to home", () -> Routes.home(this.app));
        this.shared.onPurchasesPanelVisible = () -> {
            if (presenter.state.purchasesPanelView != null) {
                presenter.state.purchasesPanelView.update();
            }
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private HomeViewData buildViewData() {
        var state = this.presenter.state;
        viewData.nickName = state.nickName != null ? state.nickName : "";
        viewData.cartCount = String.valueOf(state.cartItemCount);
        viewData.errorMessage = state.errorMessage;
        viewData.productsPanelEl = state.productsPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.purchasesPanelEl = state.purchasesPanelView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.contentViewEl = state.contentView instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        return viewData;
    }
}
