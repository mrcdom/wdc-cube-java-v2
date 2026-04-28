package br.com.wdc.framework.cube;

public class AbstractCubePresenter<A extends CubeApplication> implements CubePresenter {

    public final A app;

    protected CubeView view;

    public AbstractCubePresenter(A app) {
        this.app = app;
    }

    public final CubeView view() {
        return view;
    }

    @Override
    public void release() {
        if (view != null) {
            view.release();
            view = null;
        }
    }

    public void update() {
        if (view != null) {
            view.update();
        }
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
