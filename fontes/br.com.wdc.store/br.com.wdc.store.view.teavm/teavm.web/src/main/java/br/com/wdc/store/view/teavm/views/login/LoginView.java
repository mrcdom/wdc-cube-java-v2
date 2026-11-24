package br.com.wdc.store.view.teavm.views.login;

import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.view.teavm.app.StoreTeaVMApplication;
import br.com.wdc.store.view.teavm.commons.VNode;
import br.com.wdc.store.view.teavm.commons.views.login.LoginSharedView;
import br.com.wdc.store.view.teavm.views.AbstractVDomView;

/**
 * Login view adapter for teavm.web (local mode).
 * Delegates rendering to {@link LoginSharedView} and wires presenter actions.
 */
public class LoginView extends AbstractVDomView<LoginScope> {

    private final LoginSharedView shared;

    public LoginView(StoreTeaVMApplication app, LoginScope scope) {
        super("login", app, scope);

        this.shared = new LoginSharedView();
        this.shared.scopeSupplier = () -> scope;
        this.shared.onEnter = (userName, password) -> safeAction("Login", () -> {
            scope.onEnter.run(userName, password);
        });
    }

    @Override
    protected VNode render() {
        return shared.render();
    }
}
