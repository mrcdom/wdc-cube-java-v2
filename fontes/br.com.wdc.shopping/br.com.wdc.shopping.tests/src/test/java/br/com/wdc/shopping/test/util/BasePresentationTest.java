package br.com.wdc.shopping.test.util;

import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public class BasePresentationTest extends BaseBusinessTest {

    protected ShoppingApplicationMock app;

    @Override
    public void before() {
        super.before();
        this.app = new ShoppingApplicationMock();
    }

    @Override
    public void after() {
        this.app.release();
    }

}
