package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.RootPresenter;
import br.com.wdc.shopping.presentation.presenter.RootViewState;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class RootViewMock extends AbstractViewMock<RootPresenter> {

    public static RootViewMock cast(CubeView view) {
        var cls = RootViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (RootViewMock) view;
    }

    public RootViewState state;

    public RootViewMock(ShoppingApplicationMock app, RootPresenter presenter) {
        super(app, presenter);
        this.state = presenter.state;
    }

}
