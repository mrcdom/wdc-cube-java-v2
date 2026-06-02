package br.com.wdc.shopping.view.remote.shell.teavm.views.home;

import org.teavm.jso.JSBody;

import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView;
import br.com.wdc.shopping.view.teavm.commons.views.home.HomeSharedView.HomeViewData;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;

public class HomeView extends AbstractRemoteView {

    public static final String VIEW_ID = "473dbdd7a36a";

    private static final int ON_EXIT = 1;
    private static final int ON_OPEN_CART = 2;

    private final HomeSharedView shared;
    private final HomeViewData viewData = new HomeViewData();

    public HomeView(String vsid) {
        super(vsid);

        this.shared = new HomeSharedView();
        this.shared.stateSupplier = this::buildViewData;
        this.shared.requestUpdate = this::forceUpdate;
        this.shared.onExit = () -> submit(ON_EXIT);
        this.shared.onOpenCart = () -> submit(ON_OPEN_CART);
        this.shared.onNavigateBack = HomeView::historyBack;
        this.shared.onPurchasesPanelVisible = () -> {
            var purchasesVsid = state().getString("purchasesPanelViewId");
            if (purchasesVsid != null && !purchasesVsid.isEmpty()) {
                // The purchases panel will re-render on next server push
            }
        };
    }

    @Override
    protected VNode render() {
        return shared.renderTree();
    }

    private HomeViewData buildViewData() {
        var scope = state();
        viewData.nickName = scope.getString("nickName", "");
        viewData.cartCount = String.valueOf(scope.getInt("cartItemCount"));
        viewData.errorMessage = scope.getString("errorMessage");

        var productsPanelVsid = scope.getString("productsPanelViewId");
        var purchasesPanelVsid = scope.getString("purchasesPanelViewId");
        var contentVsid = scope.getString("contentViewId");

        viewData.productsPanelEl = getChildViewElement(productsPanelVsid);
        viewData.purchasesPanelEl = getChildViewElement(purchasesPanelVsid);
        viewData.contentViewEl = getChildViewElement(contentVsid);
        return viewData;
    }

    @JSBody(params = {}, script = "history.back();")
    private static native void historyBack();
}
