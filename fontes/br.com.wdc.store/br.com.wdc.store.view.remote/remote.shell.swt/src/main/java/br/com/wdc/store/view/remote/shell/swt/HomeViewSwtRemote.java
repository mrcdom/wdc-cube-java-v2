package br.com.wdc.store.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.impl.HomeViewSwt;

class HomeViewSwtRemote extends HomeViewSwt {
	
	public static final String CID = HomeScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	HomeViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.scopeSupplier = this::scopeSupplierImpl;
		this.onExit = this::callOnExit;
		this.onOpenCart = this::callOnOpenCart;
	}

	private void callOnExit() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnOpenCart() {
		this.ctx.submitEvent(this.vsid, 2, Map.of());
	}

	private HomeScope scopeSupplierImpl() {
		var s = this.ctx.scope(this.vsid);
		if (s == null) {
			return null;
		}
		var scope = new HomeScope();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			scope.content.set(Scope.drawnBy(this.ctx.viewLookup(contentId)));
		}
		// The panels are segments drawn by views of their own: the host refers to each by the id of its view
		var productsPanelId = s.getString("productsPanelViewId");
		if (productsPanelId != null) {
			var panel = new HomeScope.ProductsPanelScope();
			panel.attachView(this.ctx.viewLookup(productsPanelId));
			scope.productsPanel.set(panel);
		}
		var purchasesPanelId = s.getString("purchasesPanelViewId");
		if (purchasesPanelId != null) {
			var panel = new HomeScope.PurchasesPanelScope();
			panel.attachView(this.ctx.viewLookup(purchasesPanelId));
			scope.purchasesPanel.set(panel);
		}
		scope.nickName.set(s.getString("nickName"));
		var cartCount = s.getLong("cartItemCount");
		scope.cartItemCount.set(cartCount != null ? cartCount.intValue() : 0);
		scope.errorMessage.set(s.getString("errorMessage"));
		var code = s.getLong("errorCode");
		scope.errorCode.set(code != null ? code.intValue() : 0);
		return scope;
	}
}
