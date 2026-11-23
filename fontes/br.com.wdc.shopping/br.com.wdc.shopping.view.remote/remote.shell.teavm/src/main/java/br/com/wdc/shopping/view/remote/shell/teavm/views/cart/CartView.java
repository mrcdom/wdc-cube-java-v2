package br.com.wdc.shopping.view.remote.shell.teavm.views.cart;

import java.util.ArrayList;
import java.util.List;

import org.teavm.jso.dom.html.HTMLElement;

import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartScope;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.cart.CartSharedView;

public class CartView extends AbstractRemoteView {

    public static final String VIEW_ID = CartScope.CLASS_ID;

    private static final int ON_BUY = 1;
    private static final int ON_BACK = 3;

    private final CartSharedView shared;
    private final CartScope adaptedState = new CartScope();

    public CartView(String vsid) {
        super(vsid);

        this.shared = new CartSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.onBack = () -> submit(ON_BACK);
        this.shared.onBuy = () -> submit(ON_BUY);
        // The rows are views of their own (CartItemView), referred to by id in the cart's order
        this.shared.rowElements = this::rowElements;
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private CartScope adaptState() {
        ViewScope scope = state();
        adaptedState.errorMessage.set(scope.getString("errorMessage"));
        adaptedState.total.set(scope.getDouble("total"));
        return adaptedState;
    }

    private List<HTMLElement> rowElements() {
        var result = new ArrayList<HTMLElement>();
        if (state().getState().get("rowsViewIds") instanceof List<?> ids) {
            for (var id : ids) {
                var element = getChildViewElement(String.valueOf(id));
                if (element != null) {
                    result.add(element);
                }
            }
        }
        return result;
    }
}
