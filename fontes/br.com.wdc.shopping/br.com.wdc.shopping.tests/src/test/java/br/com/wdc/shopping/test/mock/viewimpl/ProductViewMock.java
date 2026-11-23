package br.com.wdc.shopping.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

@SuppressWarnings("java:S106")
public class ProductViewMock extends AbstractViewMock<ProductScope> {

    public static ProductViewMock cast(CubeView view) {
        var cls = ProductViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductViewMock) view;
    }

    public ProductViewMock(ShoppingApplicationMock app, ProductScope state) {
        super(app, state);
    }

    public void printProduct() {
        System.out.println("PRODUCT #" + this.state.product.get().id);
        System.out.println("Name: " + this.state.product.get().name);
        System.out.println("Price: " + this.state.product.get().price);
        System.out.println("Description: " + this.state.product.get().description);
        System.out.println("Image: " + this.state.product.get().image);
        System.out.println();
    }

}
