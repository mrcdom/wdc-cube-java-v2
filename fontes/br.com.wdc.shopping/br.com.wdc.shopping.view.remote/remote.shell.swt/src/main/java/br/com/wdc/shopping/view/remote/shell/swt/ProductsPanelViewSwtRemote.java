package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter.ProductsPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.products.structs.ProductInfo;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.ProductsPanelViewSwt;

class ProductsPanelViewSwtRemote extends ProductsPanelViewSwt {
	
	public static final String CID = "a1b2c3d4e5f6";

	final String vsid;
	final RemoteViewContext ctx;

	ProductsPanelViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onOpenProduct = this::callOnOpenProduct;
	}

	private void callOnOpenProduct(long id) {
		this.ctx.submitEvent(this.vsid, 1, Map.of("p.productId", id));
	}

	private ProductsPanelViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new ProductsPanelViewState();
		var rawList = s.getList("products");
		if (!rawList.isEmpty()) {
			var products = new ArrayList<ProductInfo>(rawList.size());
			for (var raw : rawList) {
				var p = new ProductInfo();
				p.id = CoerceUtils.asLong(raw.get("id"), 0L);
				p.name = (String) raw.get("name");
				p.price = CoerceUtils.asDouble(raw.get("price"), 0.0);
				p.image = (String) raw.get("image");
				products.add(p);
			}
			state.products = products;
		} else {
			state.products = Collections.emptyList();
		}
		return state;
	}
}
