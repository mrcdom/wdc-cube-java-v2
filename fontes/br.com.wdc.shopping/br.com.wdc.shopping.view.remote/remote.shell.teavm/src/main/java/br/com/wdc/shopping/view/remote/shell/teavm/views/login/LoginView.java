package br.com.wdc.shopping.view.remote.shell.teavm.views.login;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginViewState;
import br.com.wdc.shopping.view.teavm.commons.views.login.LoginSharedView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.AbstractRemoteView;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.DataSecurity;
import br.com.wdc.shopping.view.remote.shell.teavm.bridge.ViewScope;
import br.com.wdc.shopping.view.teavm.commons.VNode;

/**
 * Login view adapter for remote.shell.teavm (remote mode).
 * Delegates rendering to {@link LoginSharedView} and wires remote bridge actions.
 */
public class LoginView extends AbstractRemoteView {

    public static final String VIEW_ID = "c677cda52d14";

    private static final int ON_ENTER = 1;

    private final LoginSharedView shared;
    private final LoginViewState adaptedState = new LoginViewState();

    public LoginView(String vsid) {
        super(vsid);

        this.shared = new LoginSharedView();
        this.shared.stateSupplier = this::adaptState;
        this.shared.onEnter = (userName, password) -> {
            setFormField("userName", userName);
            DataSecurity.cipher(password, encryptedPassword -> {
                setFormField("password", encryptedPassword);
                submit(ON_ENTER);
            });
        };
    }

    @Override
    protected VNode render() {
        return shared.render();
    }

    private LoginViewState adaptState() {
        ViewScope scope = state();
        adaptedState.loading = scope.getBoolean("loading");
        adaptedState.errorMessage = scope.getString("errorMessage");
        adaptedState.errorCode = scope.getInt("errorCode");
        adaptedState.userName = scope.getString("userName");
        return adaptedState;
    }
}
