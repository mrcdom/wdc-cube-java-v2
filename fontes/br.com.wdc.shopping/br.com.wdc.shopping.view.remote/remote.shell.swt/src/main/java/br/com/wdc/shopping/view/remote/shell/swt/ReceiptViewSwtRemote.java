package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.ReceiptPresenter.ReceiptViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptForm;
import br.com.wdc.shopping.presentation.presenter.restricted.receipt.structs.ReceiptItem;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.ReceiptViewSwt;

class ReceiptViewSwtRemote extends ReceiptViewSwt {

	final String vsid;
	final RemoteViewContext ctx;

	ReceiptViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onOpenProducts = this::callOnOpenProducts;
	}

	private void callOnOpenProducts() {
		this.ctx.submitEvent(this.vsid, 1, Map.of());
	}

	private ReceiptViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new ReceiptViewState();
		state.notifySuccess = Boolean.TRUE.equals(s.getBoolean("notifySuccess"));
		var raw = s.getMap("receipt");
		if (raw != null) {
			var form = new ReceiptForm();
			form.date = CoerceUtils.asLong(raw.get("date"), 0L);
			form.total = CoerceUtils.asDouble(raw.get("total"), 0.0);
			@SuppressWarnings("unchecked")
			var rawItems = (List<Map<String, Object>>) raw.get("items");
			if (rawItems != null) {
				var items = new ArrayList<ReceiptItem>(rawItems.size());
				for (var ri : rawItems) {
					var item = new ReceiptItem();
					item.description = CoerceUtils.asString(ri.get("description"));
					item.value = CoerceUtils.asDouble(ri.get("value"), 0.0);
					item.quantity = CoerceUtils.asInteger(ri.get("quantity"), 0);
					items.add(item);
				}
				form.items = items;
			}
			state.receipt = form;
		}
		return state;
	}
}
