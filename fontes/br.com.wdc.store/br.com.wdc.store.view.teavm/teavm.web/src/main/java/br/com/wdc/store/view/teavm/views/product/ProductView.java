package br.com.wdc.store.view.teavm.views.product;

import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.product.ProductSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

public class ProductView extends AbstractVDomView<ProductScope> {

    private final ProductSharedView shared;

    public ProductView(StoreTeaVMApplication app, ProductScope state) {
        super("product", app, state);

        this.shared = new ProductSharedView();
        this.shared.stateSupplier = () -> state;
        this.shared.imageResolver = app::resolveImageUrl;
        this.shared.requestUpdate = this::update;
        this.shared.onBack = () -> safeAction("Back", state.onOpenProducts::run);
        this.shared.onAddToCart = qty -> safeAction("AddToCart", () -> state.onAddToCart.run(qty));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
