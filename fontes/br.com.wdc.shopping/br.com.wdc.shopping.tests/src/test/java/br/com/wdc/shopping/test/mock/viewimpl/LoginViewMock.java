package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter.LoginScope;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class LoginViewMock extends AbstractViewMock<LoginScope> {

    public static LoginViewMock cast(CubeView view) {
        var cls = LoginViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (LoginViewMock) view;
    }


    public LoginViewMock(ShoppingApplicationMock app, LoginScope state) {
        super(app, state);
    }

}
