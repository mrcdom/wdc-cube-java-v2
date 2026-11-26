package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.store.presentation.presenter.restricted.home.HomeScope.PurchasesPanelScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class PurchasesPanelViewMock extends AbstractViewMock<PurchasesPanelScope> {

    public static PurchasesPanelViewMock cast(Object view) {
        var cls = PurchasesPanelViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (PurchasesPanelViewMock) view;
    }

    public PurchasesPanelViewMock(StoreApplicationMock app, PurchasesPanelScope scope) {
        super(app, scope);
    }
}
