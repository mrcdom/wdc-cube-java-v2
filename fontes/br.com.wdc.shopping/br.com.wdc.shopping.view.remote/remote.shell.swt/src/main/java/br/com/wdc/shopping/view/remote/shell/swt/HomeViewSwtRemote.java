package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.cube.Scope;
import br.com.wdc.shopping.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.HomeViewSwt;

class HomeViewSwtRemote extends HomeViewSwt {
	
	public static final String CID = HomeScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	HomeViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onExit = this::callOnExit;
		this.onOpenCart = this::callOnOpenCart;
	}

	private void callOnExit() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnOpenCart() {
		this.ctx.submitEvent(this.vsid, 2, Map.of());
	}

	private HomeScope stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new HomeScope();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			state.content.set(Scope.drawnBy(this.ctx.viewLookup(contentId)));
		}
		// The panels are segments drawn by views of their own: the host refers to each by the id of its view
		var productsPanelId = s.getString("productsPanelViewId");
		if (productsPanelId != null) {
			var panel = new HomeScope.ProductsPanelScope();
			panel.attachView(this.ctx.viewLookup(productsPanelId));
			state.productsPanel.set(panel);
		}
		var purchasesPanelId = s.getString("purchasesPanelViewId");
		if (purchasesPanelId != null) {
			var panel = new HomeScope.PurchasesPanelScope();
			panel.attachView(this.ctx.viewLookup(purchasesPanelId));
			state.purchasesPanel.set(panel);
		}
		state.nickName.set(s.getString("nickName"));
		var cartCount = s.getLong("cartItemCount");
		state.cartItemCount.set(cartCount != null ? cartCount.intValue() : 0);
		state.errorMessage.set(s.getString("errorMessage"));
		var code = s.getLong("errorCode");
		state.errorCode.set(code != null ? code.intValue() : 0);
		return state;
	}
}
