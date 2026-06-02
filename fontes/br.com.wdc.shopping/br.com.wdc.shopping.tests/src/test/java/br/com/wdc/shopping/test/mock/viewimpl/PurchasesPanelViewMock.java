package br.com.wdc.shopping.test.mock.viewimpl;

import static br.com.wdc.shopping.test.mock.viewimpl.AbstractViewMock.INSTANCE_ID_GEN;

import org.junit.Assert;

import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter;
import br.com.wdc.shopping.presentation.presenter.restricted.home.purchases.PurchasesPanelPresenter.PurchasesPanelViewState;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class PurchasesPanelViewMock implements CubeView {

    public static PurchasesPanelViewMock cast(CubeView view) {
        var cls = PurchasesPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (PurchasesPanelViewMock) view;
    }

    public final ShoppingApplicationMock app;
    public final PurchasesPanelPresenter presenter;
    public PurchasesPanelViewState state;

    public PurchasesPanelViewMock(PurchasesPanelPresenter presenter) {
        this.app = (ShoppingApplicationMock) presenter.app;
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
