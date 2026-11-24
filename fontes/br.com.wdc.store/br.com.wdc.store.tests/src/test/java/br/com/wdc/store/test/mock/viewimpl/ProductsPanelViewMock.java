package br.com.wdc.store.test.mock.viewimpl;

import static br.com.wdc.store.test.mock.viewimpl.AbstractViewMock.INSTANCE_ID_GEN;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.ProductsPanelScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class ProductsPanelViewMock implements CubeView {

    public static ProductsPanelViewMock cast(CubeView view) {
        var cls = ProductsPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (ProductsPanelViewMock) view;
    }

    public final StoreApplicationMock app;
    public ProductsPanelScope state;

    public ProductsPanelViewMock(StoreApplicationMock app, ProductsPanelScope state) {
        this.app = app;
        this.state = state;
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
