package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class ProductsPanelReactViewImpl extends GenericViewImpl {

    protected ProductsPanelPresenter presenter;

    public ProductsPanelReactViewImpl(ProductsPanelPresenter presenter) {
        super(presenter.app, "a1b2c3d4e5f6");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> presenter.onOpenProduct(CoerceUtils.asLong(formData.get("p.productId")));
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
