package br.com.wdc.shopping.view.remote.shell.teavm.views.home;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.ProductsPanelSharedView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;

public class ProductsPanelView extends AbstractRemoteView {

    public static final String VIEW_ID = "a1b2c3d4e5f6";

    private static final int ON_OPEN_PRODUCT = 1;

    private final ProductsPanelSharedView shared;
    private final ProductsPanelViewState adaptedState = new ProductsPanelViewState();

    public ProductsPanelView(String vsid) {
        super(vsid);

        this.shared = new ProductsPanelSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.onOpenProduct = id -> {
            setFormField("p.productId", id);
            submit(ON_OPEN_PRODUCT);
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private ProductsPanelViewState adaptState() {
        ViewScope scope = state();
        adaptedState.products = getProducts(scope);
        return adaptedState;
    }

    private List<ProductInfo> getProducts(ViewScope scope) {
        if (scope == null) return List.of();
        var v = scope.getState().get("products");
        if (v instanceof List<?> list) {
            var result = new ArrayList<ProductInfo>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m && m.containsKey("id")) {
                    result.add(mapToProductInfo(m));
                }
            }
            return result;
        }
        return List.of();
    }

    private ProductInfo mapToProductInfo(Map<?, ?> map) {
        var info = new ProductInfo();
        info.id = CoerceUtils.asLong(map.get("id"), 0L);
        info.name = CoerceUtils.asString(map.get("name"), "");
        info.image = CoerceUtils.asString(map.get("image"), "");
        var priceVal = CoerceUtils.asNumber(map.get("price"));
        info.price = priceVal != null ? priceVal.doubleValue() : 0;
        info.description = CoerceUtils.asString(map.get("description"), "");
        return info;
    }
}
