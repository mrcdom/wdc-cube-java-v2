package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class ProductReactViewImpl extends GenericViewImpl {

    protected ProductPresenter presenter;

    public ProductReactViewImpl(ProductPresenter presenter) {
        super(presenter.app, "48b693f67410");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> presenter.onOpenProducts();
        case 2 -> presenter.onAddToCart(CoerceUtils.asInteger(formData.get("p.quantity")));
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
