package br.com.wdc.shopping.view.remote.shell.swt;

import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootViewState;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.RootViewSwt;

class RootViewSwtRemote extends RootViewSwt {

	public static final String CID = "f2d345c4a610";

	final String vsid;
	final RemoteViewContext ctx;

	RootViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
	}

	private RootViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new RootViewState();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			state.contentView = this.ctx.viewLookup(contentId);
		}
		state.errorMessage = s.getString("errorMessage");
		return state;
	}
}
