package br.com.wdc.store.view.remote.shell.swt;

import br.com.wdc.framework.cube.Scope;
import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.impl.RootViewSwt;

class RootViewSwtRemote extends RootViewSwt {

	public static final String CID = RootScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	RootViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.scopeSupplier = this::scopeSupplierImpl;
	}

	private RootScope scopeSupplierImpl() {
		var s = this.ctx.scope(this.vsid);
		if (s == null) {
			return null;
		}
		var scope = new RootScope();
		var contentId = s.getString("contentViewId");
		if (contentId != null) {
			scope.content.set(this.ctx.drawnBy(new Scope() {
			}, contentId));
		}
		scope.errorMessage.set(WireMessages.messageOf(s, "errorMessage"));
		return scope;
	}
}
