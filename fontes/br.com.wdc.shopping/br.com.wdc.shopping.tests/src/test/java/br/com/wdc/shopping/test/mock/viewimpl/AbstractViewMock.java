package br.com.wdc.shopping.test.mock.viewimpl;

import org.apache.commons.lang3.mutable.MutableInt;

import br.com.wdc.framework.cube.CubePresenter;
import br.com.wdc.framework.cube.CubeView;
import br.com.wdc.shopping.test.mock.ShoppingApplicationMock;

public abstract class AbstractViewMock<P extends CubePresenter> implements CubeView {

    public static final MutableInt INSTANCE_ID_GEN = new MutableInt();

    /*
     * Fields
     */

    public boolean released;

    public final ShoppingApplicationMock app;

    public final P presenter;

    /*
     * Constructor
     */

    protected AbstractViewMock(ShoppingApplicationMock app, P presenter) {
        this.app = app;
        this.presenter = presenter;
    }

    /*
     * API
     */

    @Override
    public void release() {
        this.released = true;
    }

    @Override
    public void update() {

    }

    @Override
    public String instanceId() {
        return String.valueOf(INSTANCE_ID_GEN.incrementAndGet());
    }

}
