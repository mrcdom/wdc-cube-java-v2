package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class CartReactViewImpl extends GenericViewImpl {

    protected CartPresenter presenter;

    public CartReactViewImpl(CartPresenter presenter) {
        super(presenter.app, "7eb485e5f843");
        this.presenter = presenter;
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        switch (eventCode) {
        case 1 -> presenter.onBuy();
        case 2 -> presenter.onRemoveProduct(CoerceUtils.asLong(formData.get("p.productId")));
        case 3 -> presenter.onOpenProducts();
        default -> new AssertionError("eventCode(" + eventCode + ") not handled");
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
