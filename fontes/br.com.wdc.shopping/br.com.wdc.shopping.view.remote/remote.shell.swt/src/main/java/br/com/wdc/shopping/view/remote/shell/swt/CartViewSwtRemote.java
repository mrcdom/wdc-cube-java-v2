package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Composite;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.CartViewSwt;

class CartViewSwtRemote extends CartViewSwt {
	
	public static final String CID = CartScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	CartViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onBuy = this::callOnBuy;
		this.onOpenProducts = this::callOnOpenProducts;
		// The rows are views of their own (CartItemViewSwtRemote), referred to by id in the cart's order
		this.rowElements = this::rowElementsImpl;
	}

	private void callOnBuy() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 3, Map.of());
	}

	private CartScope stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new CartScope();
		state.errorMessage.set(s.getString("errorMessage"));
		var code = s.getLong("errorCode");
		state.errorCode.set(code != null ? code.intValue() : 0);
		var total = s.getDouble("total");
		state.total.set(total != null ? total : 0.0);
		return state;
	}

	private List<Composite> rowElementsImpl() {
		var s = this.ctx.viewState(this.vsid);
		var result = new ArrayList<Composite>();
		if (s != null && s.fields().get("rowsViewIds") instanceof List<?> ids) {
			for (var id : ids) {
				var row = this.ctx.viewLookup(String.valueOf(id));
				if (row != null) {
					result.add(row.getElement());
				}
			}
		}
		return result;
	}
}
