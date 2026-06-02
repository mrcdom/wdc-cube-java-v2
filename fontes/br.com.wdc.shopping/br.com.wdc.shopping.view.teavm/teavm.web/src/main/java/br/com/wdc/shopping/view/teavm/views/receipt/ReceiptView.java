package br.com.wdc.shopping.view.teavm.views.receipt;

import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.receipt.ReceiptSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class ReceiptView extends AbstractVDomView<ReceiptPresenter> {

    private final ReceiptSharedView shared;

    public ReceiptView(ReceiptPresenter presenter) {
        super("receipt", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new ReceiptSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.onBack = () -> safeAction("Back", presenter::onOpenProducts);
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
