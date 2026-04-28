package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class ReceiptReactViewImpl extends GenericViewImpl {

    protected ReceiptPresenter presenter;

    public ReceiptReactViewImpl(ReceiptPresenter presenter) {
        super(presenter.app, "e8d0bd8ae3bc");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        if (eventCode == 1) {
            presenter.onOpenProducts();
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
