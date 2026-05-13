package br.com.wdc.shopping.view.react.skeleton.viewimpl;

import java.util.Map;

import br.com.wdc.framework.commons.lang.CoerceUtils;
import br.com.wdc.framework.commons.serialization.ExtensibleObjectOutput;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.view.react.skeleton.util.GenericViewImpl;

public class LoginReactViewImpl extends GenericViewImpl {

    protected LoginPresenter presenter;

    public LoginReactViewImpl(LoginPresenter presenter) {
        super(presenter.app, "c677cda52d14");
        this.presenter = presenter;
    }

    @Override
    public void syncClientToServer(Map<String, Object> formData) {
        var state = this.presenter.state;

        //@formatter:off
        var fn = "userName";
        if (formData.containsKey(fn)) {
            state.userName = CoerceUtils.asString(formData.get(fn));
        }

        fn = "password";
        if (formData.containsKey(fn)) {
            state.password = this.app.getDataSecurity().b64Decipher(CoerceUtils.asString(formData.get(fn)));
        }
        //@formatter:on
    }

    @Override
    public void submit(int eventCode, int eventQtde, Map<String, Object> formData) throws Exception {
        if (eventCode == 1) {
            presenter.onEnter();
        }
    }

    @Override
    public void writeState(ExtensibleObjectOutput json) {
        this.presenter.state.write(instanceId, json);
    }

}
