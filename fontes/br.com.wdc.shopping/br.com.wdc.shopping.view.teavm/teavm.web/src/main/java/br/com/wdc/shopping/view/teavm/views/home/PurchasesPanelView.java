package br.com.wdc.shopping.view.teavm.views.home;

import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.PurchasesPanelSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class PurchasesPanelView extends AbstractVDomView<PurchasesPanelPresenter> {

    private final PurchasesPanelSharedView shared;

    public PurchasesPanelView(PurchasesPanelPresenter presenter) {
        super("purchases-panel", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new PurchasesPanelSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.requestUpdate = this::update;
        this.shared.onOpenReceipt = id -> safeAction("Open receipt", () -> presenter.onOpenReceipt(id));
        this.shared.onPageChange = page -> safeAction("Page change", () -> presenter.onPageChange(page));
        this.shared.onPageSizeChanged = capacity -> safeAction("PageSize", () -> presenter.onItemSizeCapacityChanged(capacity));
    }

    @Override
    public void doUpdate() {
        super.doUpdate();
        shared.afterUpdate();
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
