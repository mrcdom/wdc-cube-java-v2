package br.com.wdc.store.view.remote.shell.swt;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.impl.ProductViewSwt;

class ProductViewSwtRemote extends ProductViewSwt {
	
	public static final String CID = ProductScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	ProductViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.scopeSupplier = this::scopeSupplierImpl;
		this.onOpenProducts = this::callOnOpenProducts;
		this.onAddToCart = this::callOnAddToCart;
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private void callOnAddToCart(int qty) {
		this.ctx.submitEvent(this.vsid, 2, Map.of("p.quantity", qty));
	}

	private ProductScope scopeSupplierImpl() {
		var s = this.ctx.scope(this.vsid);
		if (s == null) {
			return null;
		}
		var scope = new ProductScope();
		scope.errorMessage.set(WireMessages.messageOf(s, "errorMessage"));
		var code = s.getLong("errorCode");
		scope.errorCode.set(code != null ? code.intValue() : 0);
		var raw = s.getMap("product");
		if (raw != null) {
			var p = new ProductInfo();
			p.id = CoerceUtils.asLong(raw.get("id"), 0L);
			p.name = CoerceUtils.asString(raw.get("name"));
			p.description = CoerceUtils.asString(raw.get("description"));
			p.price = CoerceUtils.asDouble(raw.get("price"), 0.0);
			p.image = CoerceUtils.asString(raw.get("image"));
			scope.product.set(p);
		}
		return scope;
	}
}
