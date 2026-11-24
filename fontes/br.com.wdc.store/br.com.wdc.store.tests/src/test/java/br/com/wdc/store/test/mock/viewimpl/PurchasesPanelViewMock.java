package br.com.wdc.store.test.mock.viewimpl;

import static br.com.wdc.store.test.mock.viewimpl.AbstractViewMock.INSTANCE_ID_GEN;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.PurchasesPanelScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class PurchasesPanelViewMock implements CubeView {

    public static PurchasesPanelViewMock cast(CubeView view) {
        var cls = PurchasesPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (PurchasesPanelViewMock) view;
    }

    public final StoreApplicationMock app;
    public PurchasesPanelScope scope;

    public PurchasesPanelViewMock(StoreApplicationMock app, PurchasesPanelScope scope) {
        this.app = app;
        this.scope = scope;
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
