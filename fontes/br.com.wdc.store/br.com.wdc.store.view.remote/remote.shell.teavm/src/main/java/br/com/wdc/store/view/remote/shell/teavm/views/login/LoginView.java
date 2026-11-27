package br.com.wdc.store.view.remote.shell.teavm.views.login;

import br.com.wdc.framework.cube.remote.bridge.teavm.DataSecurity;
import br.com.wdc.framework.cube.remote.bridge.teavm.ViewScope;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.login.LoginSharedView;

/**
 * Login view adapter for remote.shell.teavm (remote mode).
 * Delegates rendering to {@link LoginSharedView} and wires remote bridge actions.
 */
public class LoginView extends AbstractRemoteView {

    public static final String VIEW_ID = LoginScope.CLASS_ID;

    private static final int ON_ENTER = 1;

    private final LoginSharedView shared;
    private final LoginScope adaptedScope = new LoginScope();

    public LoginView(String vsid) {
        super(vsid);

        this.shared = new LoginSharedView();
        this.shared.scopeSupplier = this::adaptScope;
        this.shared.onEnter = (userName, password) -> {
            setFormField("p.userName", userName);
            DataSecurity.cipher(password, encryptedPassword -> {
                setFormField("p.password", encryptedPassword);
                submit(ON_ENTER);
            });
        };
    }

    @Override
    protected VNode render() {
        return shared.render();
    }

    private LoginScope adaptScope() {
        ViewScope scope = scope();
        adaptedScope.loading.set(scope.getBoolean("loading"));
        adaptedScope.errorMessage.set(getMessage("errorMessage"));
        adaptedScope.errorCode.set(scope.getInt("errorCode"));
        adaptedScope.userName.set(scope.getString("userName"));
        return adaptedScope;
    }
}
