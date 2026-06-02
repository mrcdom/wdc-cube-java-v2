package br.com.wdc.shopping.view.remote.shell.teavm.views.product;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.product.ProductSharedView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;

public class ProductView extends AbstractRemoteView {

    public static final String VIEW_ID = "48b693f67410";

    private static final int ON_BACK = 1;
    private static final int ON_ADD_TO_CART = 2;

    private final ProductSharedView shared;
    private final ProductViewState adaptedState = new ProductViewState();
    private final ProductInfo adaptedProduct = new ProductInfo();

    public ProductView(String vsid) {
        super(vsid);

        this.shared = new ProductSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.requestUpdate = this::forceUpdate;
        this.shared.onBack = () -> submit(ON_BACK);
        this.shared.onAddToCart = qty -> {
            setFormField("p.quantity", qty);
            submit(ON_ADD_TO_CART);
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private ProductViewState adaptState() {
        ViewScope scope = state();
        adaptedState.errorMessage = scope.getString("errorMessage");
        adaptedState.product = adaptProduct(scope);
        return adaptedState;
    }

    private ProductInfo adaptProduct(ViewScope scope) {
        Map<String, Object> map = scope.getMap("product");
        if (map.isEmpty()) {
            adaptedProduct.id = -1;
            adaptedProduct.name = "";
            adaptedProduct.image = "";
            adaptedProduct.price = 0;
            adaptedProduct.description = "";
            return adaptedProduct;
        }
        adaptedProduct.id = CoerceUtils.asLong(map.get("id"));
        adaptedProduct.name = CoerceUtils.asString(map.get("name"), "");
        var rawImage = CoerceUtils.asString(map.get("image"));
        adaptedProduct.image = rawImage != null ? rawImage : "";
        var priceVal = CoerceUtils.asNumber(map.get("price"));
        adaptedProduct.price = priceVal != null ? priceVal.doubleValue() : 0;
        adaptedProduct.description = CoerceUtils.asString(map.get("description"), "");
        return adaptedProduct;
    }
}
