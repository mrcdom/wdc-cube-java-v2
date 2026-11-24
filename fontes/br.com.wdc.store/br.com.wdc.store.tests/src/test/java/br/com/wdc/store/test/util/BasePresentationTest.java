package br.com.wdc.store.test.util;

import br.com.wdc.store.test.mock.StoreApplicationMock;

@SuppressWarnings("java:S2187") // base class — tests are in subclasses
public class BasePresentationTest extends BaseBusinessTest {

    protected StoreApplicationMock app;

    @Override
    public void before() {
        super.before();
        this.app = new StoreApplicationMock();
    }

    @Override
    public void after() {
        this.app.release();
    }

}
