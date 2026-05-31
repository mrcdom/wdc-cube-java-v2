package br.com.wdc.shopping.view.teavm.views.login;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.login.LoginSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

/**
 * Login view adapter for teavm.web (local mode).
 * Delegates rendering to {@link LoginSharedView} and wires presenter actions.
 */
public class LoginView extends AbstractVDomView<LoginPresenter> {

    private final LoginSharedView shared;

    public LoginView(LoginPresenter presenter) {
        super("login", (ShoppingTeaVMApplication) presenter.app, presenter);

        this.shared = new LoginSharedView();
        this.shared.stateSupplier = () -> presenter.state;
        this.shared.onEnter = (userName, password) -> safeAction("Login", () -> {
            presenter.state.userName = userName;
            presenter.state.password = password;
            presenter.onEnter();
        });
    }

    @Override
    protected VNode render() {
        return shared.render();
    }
}
