package br.com.wdc.store.view.teavm.views.home;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.PurchasesPanelScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.home.PurchasesPanelSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

public class PurchasesPanelView extends AbstractVDomView<PurchasesPanelScope> {

    private final PurchasesPanelSharedView shared;

    public PurchasesPanelView(StoreTeaVMApplication app, PurchasesPanelScope scope) {
        super("purchases-panel", app, scope);

        this.shared = new PurchasesPanelSharedView();
        this.shared.stateSupplier = () -> scope;
        this.shared.requestUpdate = this::update;
        this.shared.onOpenReceipt = id -> safeAction("Open receipt", () -> scope.onOpenReceipt.run(id));
        this.shared.onPageChange = page -> safeAction("Page change", () -> scope.onPageChange.run(page));
        this.shared.onPageSizeChanged = capacity -> safeAction("PageSize", () -> scope.onItemSizeCapacityChanged.run(capacity));
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
