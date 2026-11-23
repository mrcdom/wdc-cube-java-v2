package br.com.wdc.framework.cube;

public class AbstractCubePresenter<A extends CubeApplication> extends AbstractPresenter<A> implements CubePresenter {

    public AbstractCubePresenter(A app) {
        super(app);
    }

    @Override
    public boolean applyParameters(CubeIntent intent, boolean initialization, boolean deepest) {
        return true;
    }

    @Override
    public void publishParameters(CubeIntent intent) {
        // NOOP
    }

}
