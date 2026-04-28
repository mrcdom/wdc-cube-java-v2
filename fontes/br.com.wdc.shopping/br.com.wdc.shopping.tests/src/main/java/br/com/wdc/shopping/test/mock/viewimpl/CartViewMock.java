package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.cart.CartViewState;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings({"java:S106", "java:S1192"})
public class CartViewMock extends AbstractViewMock<CartPresenter> {

    public static CartViewMock cast(CubeView view) {
        var cls = CartViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (CartViewMock) view;
    }

    public CartViewState state;

    public CartViewMock(ShoppingApplicationMock app, CartPresenter presenter) {
        super(app, presenter);
        this.state = presenter.state;
    }


}
