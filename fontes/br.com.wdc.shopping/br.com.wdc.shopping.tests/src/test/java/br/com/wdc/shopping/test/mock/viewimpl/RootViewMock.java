package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.RootPresenter.RootScope;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class RootViewMock extends AbstractViewMock<RootScope> {

    public static RootViewMock cast(CubeView view) {
        var cls = RootViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (RootViewMock) view;
    }


    public RootViewMock(ShoppingApplicationMock app, RootScope state) {
        super(app, state);
    }

}
