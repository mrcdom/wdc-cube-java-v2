package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.store.presentation.presenter.open.login.LoginScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class LoginViewMock extends AbstractViewMock<LoginScope> {

    public static LoginViewMock cast(CubeView view) {
        var cls = LoginViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (LoginViewMock) view;
    }

    public LoginViewMock(StoreApplicationMock app, LoginScope state) {
        super(app, state);
    }

}
