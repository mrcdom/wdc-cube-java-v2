package br.com.wdc.store.test.mock.viewimpl;

import org.junit.Assert;

import br.com.wdc.store.presentation.presenter.RootScope;
import br.com.wdc.store.test.mock.StoreApplicationMock;

public class RootViewMock extends AbstractViewMock<RootScope> {

    public static RootViewMock cast(Object view) {
        var cls = RootViewMock.class;
        Assert.assertNotNull("Expecting " + cls.getSimpleName() + " but this view was null", view);
        Assert.assertTrue("Expecting " + cls.getSimpleName() + " but it was " + view.getClass().getSimpleName(),
                cls.isInstance(view));
        return (RootViewMock) view;
    }

    public RootViewMock(StoreApplicationMock app, RootScope scope) {
        super(app, scope);
    }

}
