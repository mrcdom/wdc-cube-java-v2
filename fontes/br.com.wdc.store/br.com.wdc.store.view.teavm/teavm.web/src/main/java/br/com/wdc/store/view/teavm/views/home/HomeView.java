package br.com.wdc.store.view.teavm.views.home;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeKeys;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.home.HomeSharedView;
import br.com.wdc.store.view.teavm.commons.views.home.HomeSharedView.HomeViewData;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;
import br.com.wdc.store.view.teavm.views.AbstractViewTeaVM;

public class HomeView extends AbstractVDomView<HomeScope> {

    private final HomeSharedView shared;
    private final HomeViewData viewData = new HomeViewData();

    public HomeView(StoreTeaVMApplication app, HomeScope scope) {
        super("home", app, scope);

        this.shared = new HomeSharedView();
        this.shared.scopeSupplier = this::buildViewData;
        this.shared.requestUpdate = this::update;
        this.shared.onExit = () -> safeAction("Exit", scope.onExit::run);
        this.shared.onOpenCart = () -> safeAction("Open cart", scope.onOpenCart::run);
        this.shared.onNavigateBack = () -> safeAction("Back to home", () -> new HomeKeys(this.app).flip());
        this.shared.onPurchasesPanelVisible = () -> {
            var purchasesPanel = this.viewOf(this.scope.purchasesPanel.get());
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
        viewData.nickName = this.scope.nickName.get() != null ? this.scope.nickName.get() : "";
        viewData.cartCount = String.valueOf(this.scope.cartItemCount.get());
        viewData.errorMessage = this.scope.errorMessage.get();
        viewData.productsPanelEl = this.viewOf(this.scope.productsPanel.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.purchasesPanelEl = this.viewOf(this.scope.purchasesPanel.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        viewData.contentViewEl = this.viewOf(this.scope.content.get()) instanceof AbstractViewTeaVM<?> v ? v.getElement() : null;
        return viewData;
    }
}
