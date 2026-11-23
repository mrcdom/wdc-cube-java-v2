package br.com.wdc.shopping.view.teavm.views.login;

import br.com.wdc.shopping.presentation.presenter.open.login.LoginScope;
import br.com.wdc.shopping.view.teavm.app.ShoppingTeaVMApplication;
import br.com.wdc.shopping.view.teavm.commons.VNode;
import br.com.wdc.shopping.view.teavm.commons.views.login.LoginSharedView;
import br.com.wdc.shopping.view.teavm.views.AbstractVDomView;

/**
 * Login view adapter for teavm.web (local mode).
 * Delegates rendering to {@link LoginSharedView} and wires presenter actions.
 */
public class LoginView extends AbstractVDomView<LoginScope> {

    private final LoginSharedView shared;

    public LoginView(ShoppingTeaVMApplication app, LoginScope state) {
        super("login", app, state);

        this.shared = new LoginSharedView();
        this.shared.stateSupplier = () -> state;
        this.shared.onEnter = (userName, password) -> safeAction("Login", () -> {
            state.onEnter.run(userName, password);
        });
    }

    @Override
    protected VNode render() {
        return shared.render();
    }
}
