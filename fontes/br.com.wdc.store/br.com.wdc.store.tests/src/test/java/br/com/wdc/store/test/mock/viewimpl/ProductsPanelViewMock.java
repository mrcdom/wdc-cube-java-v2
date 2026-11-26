package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class ProductsPanelViewMock extends AbstractViewMock<ProductsPanelScope> {

    public static ProductsPanelViewMock cast(Object view) {
        var cls = ProductsPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductsPanelViewMock) view;
    }

    public ProductsPanelViewMock(StoreApplicationMock app, ProductsPanelScope scope) {
        super(app, scope);
    }

}
