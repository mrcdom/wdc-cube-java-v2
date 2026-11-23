package br.com.wdc.shopping.view.teavm.views.receipt;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptScope;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.receipt.ReceiptSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class ReceiptView extends AbstractVDomView<ReceiptScope> {

    private final ReceiptSharedView shared;

    public ReceiptView(ShoppingTeaVMApplication app, ReceiptScope state) {
        super("receipt", app, state);

        this.shared = new ReceiptSharedView();
        this.shared.stateSupplier = () -> state;
        this.shared.onBack = () -> safeAction("Back", state.onOpenProducts::run);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
