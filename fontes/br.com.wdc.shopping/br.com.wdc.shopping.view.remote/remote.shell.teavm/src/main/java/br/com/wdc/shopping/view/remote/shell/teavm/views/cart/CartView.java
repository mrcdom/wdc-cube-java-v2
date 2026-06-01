package br.com.wdc.shopping.view.remote.shell.teavm.views.cart;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.cart.CartSharedView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;

public class CartView extends AbstractRemoteView {

    public static final String VIEW_ID = "7eb485e5f843";

    private static final int ON_BUY = 1;
    private static final int ON_REMOVE = 2;
    private static final int ON_BACK = 3;
    private static final int ON_MODIFY_QUANTITY = 4;

    private final CartSharedView shared;
    private final CartViewState adaptedState = new CartViewState();

    public CartView(String vsid) {
        super(vsid);

        this.shared = new CartSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.onBack = () -> submit(ON_BACK);
        this.shared.onBuy = () -> submit(ON_BUY);
        this.shared.onModifyQuantity = (id, qty) -> {
            setFormField("p.productId", id);
            setFormField("p.quantity", qty);
            submit(ON_MODIFY_QUANTITY);
        };
        this.shared.onRemoveProduct = id -> {
            setFormField("p.productId", id);
            submit(ON_REMOVE);
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private CartViewState adaptState() {
        ViewScope scope = state();
        adaptedState.errorMessage = scope.getString("errorMessage");
        adaptedState.items = getItems(scope);
        return adaptedState;
    }

    private List<CartItem> getItems(ViewScope scope) {
        if (scope == null) return List.of();
        var v = scope.getState().get("items");
        if (v instanceof List<?> list) {
            var result = new ArrayList<CartItem>();
            for (var item : list) {
                if (item instanceof Map<?, ?> m) result.add(mapToCartItem(m));
            }
            return result;
        }
        return List.of();
    }

    private CartItem mapToCartItem(Map<?, ?> m) {
        var item = new CartItem();
        item.id = CoerceUtils.asLong(m.get("id"), Long.MIN_VALUE);
        item.name = CoerceUtils.asString(m.get("name"));
        item.price = CoerceUtils.asDouble(m.get("price"), 0.0);
        item.quantity = CoerceUtils.asInteger(m.get("quantity"), 1);
        return item;
    }
}
