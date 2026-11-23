package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.CartItemViewSwt;

/** A row of the cart: a view of its own, whose actions are the row's (no product id goes back). */
class CartItemViewSwtRemote extends CartItemViewSwt {

	public static final String CID = CartScope.CartItemScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	CartItemViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onRemove = () -> this.ctx.submitEvent(this.vsid, 1, Map.of());
		this.onModifyQuantity = qty -> this.ctx.submitEvent(this.vsid, 2, Map.of("p.quantity", qty));
	}

	private CartScope.CartItemScope stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var row = new CartScope.CartItemScope();
		row.name.set(s.getString("name"));
		var price = s.getDouble("price");
		row.price.set(price != null ? price : 0.0);
		var quantity = s.getLong("quantity");
		row.quantity.set(quantity != null ? quantity.intValue() : 0);
		return row;
	}
}
