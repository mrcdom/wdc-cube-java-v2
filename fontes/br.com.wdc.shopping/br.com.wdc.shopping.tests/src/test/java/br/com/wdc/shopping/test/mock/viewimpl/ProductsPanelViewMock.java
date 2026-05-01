package br.com.wdc.shopping.test.mock.viewimpl;

import static br.com.wdc.shopping.test.mock.viewimpl.AbstractViewMock.INSTANCE_ID_GEN;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.products.ProductsPanelViewState;

public class ProductsPanelViewMock implements CubeView {

    public static ProductsPanelViewMock cast(CubeView view) {
        var cls = ProductsPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductsPanelViewMock) view;
    }

    public final ProductsPanelPresenter presenter;
    public ProductsPanelViewState state;

    public ProductsPanelViewMock(ProductsPanelPresenter presenter) {
        this.presenter = presenter;
        this.state = presenter.state;
    }

    @Override
    public void release() {
    	// NOOP
    }

    @Override
    public void update() {
    	// NOOP
    }
    
    public String instanceId() {
        return String.valueOf(INSTANCE_ID_GEN.incrementAndGet());
    }

}
