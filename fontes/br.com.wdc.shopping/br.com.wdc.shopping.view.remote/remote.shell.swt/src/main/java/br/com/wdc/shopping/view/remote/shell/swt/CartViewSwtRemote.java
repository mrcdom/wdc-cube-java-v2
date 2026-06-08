package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter.CartViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.structs.CartItem;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.CartViewSwt;

class CartViewSwtRemote extends CartViewSwt {

	final String vsid;
	final RemoteViewContext ctx;

	CartViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onBuy = this::callOnBuy;
		this.onRemoveProduct = this::callOnRemoveProduct;
		this.onOpenProducts = this::callOnOpenProducts;
		this.onModifyQuantity = this::callOnModifyQuantity;
	}

	private void callOnBuy() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnRemoveProduct(Long id) {
		this.ctx.submitEvent(this.vsid, 2, Map.of("p.productId", id));
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 3, Map.of());
	}

	private void callOnModifyQuantity(Long id, Integer qty) {
		this.ctx.submitEvent(this.vsid, 4, Map.of("p.productId", id, "p.quantity", qty));
	}

	private CartViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new CartViewState();
		state.errorMessage = s.getString("errorMessage");
		var code = s.getLong("errorCode");
		state.errorCode = code != null ? code.intValue() : 0;
		var rawItems = s.getList("items");
		if (!rawItems.isEmpty()) {
			var items = new ArrayList<CartItem>(rawItems.size());
			for (var raw : rawItems) {
				var item = new CartItem();
				item.id = CoerceUtils.asLong(raw.get("id"), 0L);
				item.name = CoerceUtils.asString(raw.get("name"));
				item.image = CoerceUtils.asString(raw.get("image"));
				item.price = CoerceUtils.asDouble(raw.get("price"), 0.0);
				item.quantity = CoerceUtils.asInteger(raw.get("quantity"), 0);
				items.add(item);
			}
			state.items = items;
		} else {
			state.items = Collections.emptyList();
		}
		return state;
	}
}
