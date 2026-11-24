package br.com.wdc.store.view.teavm.views.receipt;

import br.com.wdc.store.presentation.presenter.restricted.receipt.ReceiptScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.receipt.ReceiptSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

public class ReceiptView extends AbstractVDomView<ReceiptScope> {

    private final ReceiptSharedView shared;

    public ReceiptView(StoreTeaVMApplication app, ReceiptScope scope) {
        super("receipt", app, scope);

        this.shared = new ReceiptSharedView();
        this.shared.scopeSupplier = () -> scope;
        this.shared.onBack = () -> safeAction("Back", scope.onOpenProducts::run);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
