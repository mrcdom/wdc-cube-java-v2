package br.com.wdc.shopping.view.remote.shell.swt;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.swt.SwtApp;
import br.com.wdc.shopping.view.swt.impl.LoginViewSwt;

class LoginViewSwtRemote extends LoginViewSwt {

	public static final String CID = "c677cda52d14";

	final String vsid;
	final RemoteViewContext ctx;

	LoginViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.stateSupplier = this::stateSupplierImpl;
		this.onEnter = this::callOnEnter;
	}

	private void callOnEnter(String user, String pass) {
		this.ctx.submitLogin(this.vsid, user, pass);
	}

	private LoginViewState stateSupplierImpl() {
		var s = this.ctx.viewState(this.vsid);
		if (s == null) {
			return null;
		}
		var state = new LoginViewState();
		state.userName = s.getString("userName");
		state.errorMessage = s.getString("errorMessage");
		var code = s.getLong("errorCode");
		state.errorCode = code != null ? code.intValue() : 0;
		state.loading = Boolean.TRUE.equals(s.getBoolean("loading"));
		return state;
	}
}
