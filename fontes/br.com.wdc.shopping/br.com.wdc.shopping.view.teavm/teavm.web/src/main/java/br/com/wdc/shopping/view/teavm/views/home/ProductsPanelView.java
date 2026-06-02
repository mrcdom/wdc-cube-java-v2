package br.com.wdc.shopping.view.teavm.views.home;

import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.ProductsPanelSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

public class ProductsPanelView extends AbstractVDomView<ProductsPanelPresenter> {

    private final ProductsPanelSharedView shared;

    public ProductsPanelView(ProductsPanelPresenter presenter) {
        super("products-panel", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new ProductsPanelSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.imageResolver = app::resolveImageUrl;
        this.shared.onOpenProduct = id -> safeAction("Open product", () -> presenter.onOpenProduct(id));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
