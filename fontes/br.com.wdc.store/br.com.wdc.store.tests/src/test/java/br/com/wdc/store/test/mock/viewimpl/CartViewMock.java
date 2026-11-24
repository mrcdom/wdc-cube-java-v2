package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.store.presentation.presenter.restricted.cart.CartScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

@SuppressWarnings({ "java:S106", "java:S1192" })
public class CartViewMock extends AbstractViewMock<CartScope> {

    public static CartViewMock cast(CubeView view) {
        var cls = CartViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (CartViewMock) view;
    }

    public CartViewMock(StoreApplicationMock app, CartScope scope) {
        super(app, scope);
    }

}
