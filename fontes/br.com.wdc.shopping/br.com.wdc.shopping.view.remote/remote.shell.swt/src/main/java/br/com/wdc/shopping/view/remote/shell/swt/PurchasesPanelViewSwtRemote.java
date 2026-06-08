package br.com.wdc.shopping.view.remote.shell.swt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.presentation.presenter.restricted.home.structs.PurchaseInfo;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.PurchasesPanelViewSwt;

class PurchasesPanelViewSwtRemote extends PurchasesPanelViewSwt {
	
	public static final String CID = "b3c4d5e6f7a8";

	final String vsid;
	final RemoteViewContext ctx;

	PurchasesPanelViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onOpenReceipt = this::callOnOpenReceipt;
		this.onPageChange = this::callOnPageChange;
		this.onItemSizeCapacityChanged = this::callOnItemSizeCapacityChanged;
	}

	private void callOnOpenReceipt(long id) {
		this.ctx.submitEvent(this.vsid, 1, Map.of("p.purchaseId", id));
	}

	private void callOnPageChange(int pg) {
		this.ctx.submitEvent(this.vsid, 2, Map.of("p.page", pg));
	}

	private void callOnItemSizeCapacityChanged(int cap) {
		this.ctx.submitEvent(this.vsid, 3, Map.of("p.capacity", cap));
	}

	private PurchasesPanelViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new PurchasesPanelViewState();
		var pageNum = s.getLong("page");
		state.page = pageNum != null ? pageNum.intValue() : 0;
		var pageSize = s.getLong("pageSize");
		state.pageSize = pageSize != null ? pageSize.intValue() : -1;
		var totalCount = s.getLong("totalCount");
		state.totalCount = totalCount != null ? totalCount.intValue() : 0;
		var rawList = s.getList("purchases");
		if (!rawList.isEmpty()) {
			var purchases = new ArrayList<PurchaseInfo>(rawList.size());
			for (var raw : rawList) {
				var p = new PurchaseInfo();
				p.id = CoerceUtils.asLong(raw.get("id"), 0L);
				p.date = CoerceUtils.asLong(raw.get("date"), 0L);
				p.total = CoerceUtils.asDouble(raw.get("total"), 0.0);
				@SuppressWarnings("unchecked")
				var itemNames = (List<String>) raw.get("items");
				p.items = itemNames != null ? itemNames : Collections.emptyList();
				purchases.add(p);
			}
			state.purchases = purchases;
		} else {
			state.purchases = Collections.emptyList();
		}
		return state;
	}
}
