package br.com.wdc.store.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.view.swt.AbstractViewSwt;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.impl.CartViewSwt;

class CartViewSwtRemote extends CartViewSwt {
	
	public static final String CID = CartScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	CartViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.scopeSupplier = this::scopeSupplierImpl;
		this.onBuy = this::callOnBuy;
		this.onOpenProducts = this::callOnOpenProducts;
		// The rows are views of their own (CartItemViewSwtRemote), referred to by id in the cart's order
		this.rowViews = this::rowViewsImpl;
	}

	private void callOnBuy() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 3, Map.of());
	}

	private CartScope scopeSupplierImpl() {
		var s = this.ctx.scope(this.vsid);
		if (s == null) {
			return null;
		}
		var scope = new CartScope();
		scope.errorMessage.set(s.getString("errorMessage"));
		var code = s.getLong("errorCode");
		scope.errorCode.set(code != null ? code.intValue() : 0);
		var total = s.getDouble("total");
		scope.total.set(total != null ? total : 0.0);
		return scope;
	}

	private List<AbstractViewSwt> rowViewsImpl() {
		var s = this.ctx.scope(this.vsid);
		var result = new ArrayList<AbstractViewSwt>();
		if (s != null && s.values().get("rowsViewIds") instanceof List<?> ids) {
			for (var id : ids) {
				var row = this.ctx.viewLookup(String.valueOf(id));
				if (row != null) {
					result.add(row);
				}
			}
		}
		return result;
	}
}
