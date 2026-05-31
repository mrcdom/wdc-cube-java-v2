package br.com.wdc.shopping.view.teavm.views.product;

import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.product.ProductSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class ProductView extends AbstractVDomView<ProductPresenter> {

    private final ProductSharedView shared;

    public ProductView(ProductPresenter presenter) {
        super("product", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new ProductSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.imageResolver = app::resolveImageUrl;
        this.shared.requestUpdate = this::update;
        this.shared.onBack = () -> safeAction("Back", presenter::onOpenProducts);
        this.shared.onAddToCart = qty -> safeAction("AddToCart", () -> presenter.onAddToCart(qty));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
