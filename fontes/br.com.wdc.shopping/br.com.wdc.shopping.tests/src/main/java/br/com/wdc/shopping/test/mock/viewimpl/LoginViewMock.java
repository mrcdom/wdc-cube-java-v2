package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginPresenter;
import br.com.wdc.shopping.presentation.presenter.open.login.LoginViewState;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class LoginViewMock extends AbstractViewMock<LoginPresenter> {

    public static LoginViewMock cast(CubeView view) {
        var cls = LoginViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (LoginViewMock) view;
    }

    public LoginViewState state;

    public LoginViewMock(ShoppingApplicationMock app, LoginPresenter presenter) {
        super(app, presenter);
        this.state = presenter.state;
    }

}
