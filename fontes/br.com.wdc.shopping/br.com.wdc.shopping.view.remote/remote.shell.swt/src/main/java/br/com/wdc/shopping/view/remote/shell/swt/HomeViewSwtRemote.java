package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.shopping.presentation.presenter.restricted.home.HomePresenter.HomeViewState;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.HomeViewSwt;

class HomeViewSwtRemote extends HomeViewSwt {

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

	private HomeViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new HomeViewState();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			state.contentView = this.ctx.viewLookup(contentId);
		}
		var productsPanelId = s.getString("productsPanelViewId");
		if (productsPanelId != null) {
			state.productsPanelView = this.ctx.viewLookup(productsPanelId);
		}
		var purchasesPanelId = s.getString("purchasesPanelViewId");
		if (purchasesPanelId != null) {
			state.purchasesPanelView = this.ctx.viewLookup(purchasesPanelId);
		}
		state.nickName = s.getString("nickName");
		var cartCount = s.getLong("cartItemCount");
		state.cartItemCount = cartCount != null ? cartCount.intValue() : 0;
		state.errorMessage = s.getString("errorMessage");
		var code = s.getLong("errorCode");
		state.errorCode = code != null ? code.intValue() : 0;
		return state;
	}
}
