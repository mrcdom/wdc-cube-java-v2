package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductPresenter.ProductViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.ProductViewSwt;

class ProductViewSwtRemote extends ProductViewSwt {

	final String vsid;
	final RemoteViewContext ctx;

	ProductViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onOpenProducts = this::callOnOpenProducts;
		this.onAddToCart = this::callOnAddToCart;
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnAddToCart(int qty) {
		this.ctx.submitEvent(this.vsid, 2, Map.of("p.quantity", qty));
	}

	private ProductViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new ProductViewState();
		state.errorMessage = s.getString("errorMessage");
		var code = s.getLong("errorCode");
		state.errorCode = code != null ? code.intValue() : 0;
		var raw = s.getMap("product");
		if (raw != null) {
			var p = new ProductInfo();
			p.id = CoerceUtils.asLong(raw.get("id"), 0L);
			p.name = CoerceUtils.asString(raw.get("name"));
			p.description = CoerceUtils.asString(raw.get("description"));
			p.price = CoerceUtils.asDouble(raw.get("price"), 0.0);
			p.image = CoerceUtils.asString(raw.get("image"));
			state.product = p.id != 0 ? p : null;
		}
		return state;
	}
}
