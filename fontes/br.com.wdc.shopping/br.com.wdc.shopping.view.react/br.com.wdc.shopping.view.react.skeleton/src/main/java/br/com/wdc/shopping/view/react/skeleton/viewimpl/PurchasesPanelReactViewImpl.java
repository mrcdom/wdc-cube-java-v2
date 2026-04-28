package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class PurchasesPanelReactViewImpl extends GenericViewImpl {

    protected PurchasesPanelPresenter presenter;

    public PurchasesPanelReactViewImpl(PurchasesPanelPresenter presenter) {
        super(presenter.app, "b3c4d5e6f7a8");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> presenter.onOpenReceipt(CoerceUtils.asLong(formData.get("p.purchaseId")));
        case 2 -> presenter.onPageChange(CoerceUtils.asInteger(formData.get("p.page")));
        case 3 -> presenter.onPageSizeChange(CoerceUtils.asInteger(formData.get("p.pageSize")));
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
