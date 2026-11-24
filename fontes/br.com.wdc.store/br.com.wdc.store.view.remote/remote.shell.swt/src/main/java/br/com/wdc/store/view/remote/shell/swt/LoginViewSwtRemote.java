package br.com.wdc.store.view.remote.shell.swt;

import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.view.swt.SwtApp;
import br.com.wdc.store.view.swt.impl.LoginViewSwt;

class LoginViewSwtRemote extends LoginViewSwt {

	public static final String CID = LoginScope.CLASS_ID;

	final String vsid;
	final RemoteViewContext ctx;

	LoginViewSwtRemote(SwtApp app, String vsid, RemoteViewContext ctx) {
		super(app);
		this.vsid = vsid;
		this.ctx = ctx;
		this.scopeSupplier = this::scopeSupplierImpl;
		this.onEnter = this::callOnEnter;
	}

	private void callOnEnter(String user, String pass) {
		this.ctx.submitLogin(this.vsid, user, pass);
	}

	private LoginScope scopeSupplierImpl() {
		var s = this.ctx.scope(this.vsid);
		if (s == null) {
			return null;
		}
		var scope = new LoginScope();
		scope.userName.set(s.getString("userName"));
		scope.errorMessage.set(s.getString("errorMessage"));
		var code = s.getLong("errorCode");
		scope.errorCode.set(code != null ? code.intValue() : 0);
		scope.loading.set(Boolean.TRUE.equals(s.getBoolean("loading")));
		return scope;
	}
}
