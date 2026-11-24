package br.com.wdc.store.view.teavm.views.home;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.home.ProductsPanelSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

public class ProductsPanelView extends AbstractVDomView<ProductsPanelScope> {

    private final ProductsPanelSharedView shared;

    public ProductsPanelView(StoreTeaVMApplication app, ProductsPanelScope scope) {
        super("products-panel", app, scope);

        this.shared = new ProductsPanelSharedView();
        this.shared.stateSupplier = () -> scope;
        this.shared.imageResolver = app::resolveImageUrl;
        this.shared.onOpenProduct = id -> safeAction("Open product", () -> scope.onOpenProduct.run(id));
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }
}
