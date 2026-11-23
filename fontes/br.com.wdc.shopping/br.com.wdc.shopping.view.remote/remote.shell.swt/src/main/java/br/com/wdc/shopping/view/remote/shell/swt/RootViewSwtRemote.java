package br.com.wdc.shopping.view.remote.shell.swt;

import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootScope;
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

	private RootScope stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new RootScope();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			state.contentView.set(this.ctx.viewLookup(contentId));
		}
		state.errorMessage.set(s.getString("errorMessage"));
		return state;
	}
}
