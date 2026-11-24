package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.store.presentation.presenter.restricted.products.ProductScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

@SuppressWarnings("java:S106")
public class ProductViewMock extends AbstractViewMock<ProductScope> {

    public static ProductViewMock cast(Object view) {
        var cls = ProductViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductViewMock) view;
    }

    public ProductViewMock(StoreApplicationMock app, ProductScope scope) {
        super(app, scope);
    }

    public void printProduct() {
        System.out.println("PRODUCT #" + this.scope.product.get().id);
        System.out.println("Name: " + this.scope.product.get().name);
        System.out.println("Price: " + this.scope.product.get().price);
        System.out.println("Description: " + this.scope.product.get().description);
        System.out.println("Image: " + this.scope.product.get().image);
        System.out.println();
    }

}
